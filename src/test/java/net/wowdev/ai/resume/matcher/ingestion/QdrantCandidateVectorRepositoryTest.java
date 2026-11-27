package net.wowdev.ai.resume.matcher.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.SettableFuture;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.PayloadSchemaType;
import io.qdrant.client.grpc.Common.PointId;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.UpdateResult;
import java.util.List;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.CandidateVectorPoint;
import net.wowdev.ai.resume.matcher.model.CandidateVectorSnapshot;
import net.wowdev.ai.resume.matcher.repository.CandidateVectorRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;

class QdrantCandidateVectorRepositoryTest {

  @Test
  void shouldReplaceEmailPointsAndAddCurrentProfileAndExperienceDocuments() {
    VectorStore vectorStore = mock(VectorStore.class);
    QdrantClient client = mock(QdrantClient.class);
    CandidateEmbeddingDocumentBuilder builder = new CandidateEmbeddingDocumentBuilder();
    Candidate candidate =
        new Candidate(
            "mongo-123",
            " Jane@Example.com ",
            "Jane Doe",
            "Mexico",
            "Monterrey",
            "Engineer",
            "Java",
            "Remote",
            "2 years and 0 months",
            List.of());
    CandidateVectorRepository repository =
        new QdrantCandidateVectorRepository(vectorStore, client, builder, "candidate-resumes");

    repository.replaceByEmail(candidate, "jane.pdf");

    org.mockito.ArgumentCaptor<Expression> filter =
        org.mockito.ArgumentCaptor.forClass(Expression.class);
    verify(vectorStore).delete(filter.capture());
    assertEquals(
        "email",
        ((org.springframework.ai.vectorstore.filter.Filter.Key) filter.getValue().left()).key());
    assertEquals(
        "jane@example.com",
        ((org.springframework.ai.vectorstore.filter.Filter.Value) filter.getValue().right())
            .value());
    org.mockito.ArgumentCaptor<List<Document>> documents =
        org.mockito.ArgumentCaptor.forClass(List.class);
    verify(vectorStore).add(documents.capture());
    assertEquals(1, documents.getValue().size());
  }

  @Test
  void shouldProvisionEmailPayloadIndex() throws Exception {
    VectorStore vectorStore = mock(VectorStore.class);
    QdrantClient client = mock(QdrantClient.class);
    SettableFuture<UpdateResult> result = SettableFuture.create();
    result.set(UpdateResult.getDefaultInstance());
    when(client.createPayloadIndexAsync(
            eq("candidate-resumes"),
            eq("email"),
            eq(PayloadSchemaType.Keyword),
            eq(null),
            eq(true),
            eq(null),
            any(java.time.Duration.class)))
        .thenReturn(result);
    QdrantCandidateVectorRepository repository =
        new QdrantCandidateVectorRepository(
            vectorStore, client, new CandidateEmbeddingDocumentBuilder(), "candidate-resumes");

    repository.ensureEmailPayloadIndex();

    verify(client)
        .createPayloadIndexAsync(
            eq("candidate-resumes"),
            eq("email"),
            eq(PayloadSchemaType.Keyword),
            eq(null),
            eq(true),
            eq(null),
            any(java.time.Duration.class));
  }

  @Test
  void shouldSnapshotExactPointIdPayloadAndVectorAndRestoreIt() throws Exception {
    VectorStore vectorStore = mock(VectorStore.class);
    QdrantClient client = mock(QdrantClient.class);
    Points.RetrievedPoint retrieved =
        Points.RetrievedPoint.newBuilder()
            .setId(PointId.newBuilder().setUuid("point-uuid"))
            .putPayload(
                "email", JsonWithInt.Value.newBuilder().setStringValue("jane@example.com").build())
            .putPayload(
                "text", JsonWithInt.Value.newBuilder().setStringValue("profile text").build())
            .setVectors(
                Points.VectorsOutput.newBuilder()
                    .setVector(
                        Points.VectorOutput.newBuilder()
                            .setDense(
                                Points.DenseVector.newBuilder().addAllData(List.of(0.25F, 0.75F)))))
            .build();
    when(client.scrollAsync(any(Points.ScrollPoints.class), any(java.time.Duration.class)))
        .thenReturn(SettableFuture.create());
    SettableFuture<Points.ScrollResponse> scroll = SettableFuture.create();
    scroll.set(Points.ScrollResponse.newBuilder().addResult(retrieved).build());
    when(client.scrollAsync(any(Points.ScrollPoints.class), any(java.time.Duration.class)))
        .thenReturn(scroll);
    SettableFuture<UpdateResult> upsert = SettableFuture.create();
    upsert.set(UpdateResult.getDefaultInstance());
    when(client.upsertAsync(
            eq("candidate-resumes"), any(List.class), any(java.time.Duration.class)))
        .thenReturn(upsert);
    QdrantCandidateVectorRepository repository =
        new QdrantCandidateVectorRepository(
            vectorStore, client, new CandidateEmbeddingDocumentBuilder(), "candidate-resumes");

    CandidateVectorSnapshot snapshot = repository.snapshotByEmail(" Jane@Example.com ");
    repository.restoreByEmail("jane@example.com", snapshot);

    assertEquals(1, snapshot.points().size());
    assertEquals("point-uuid", snapshot.points().getFirst().id());
    assertEquals("profile text", snapshot.points().getFirst().payload().get("text"));
    assertEquals(List.of(0.25F, 0.75F), snapshot.points().getFirst().vectors().get(""));
    verify(vectorStore).delete(any(Expression.class));
    ArgumentCaptor<List<PointStruct>> restoredPoints = ArgumentCaptor.forClass(List.class);
    verify(client)
        .upsertAsync(
            eq("candidate-resumes"), restoredPoints.capture(), any(java.time.Duration.class));
    PointStruct restored = restoredPoints.getValue().getFirst();
    assertEquals("point-uuid", restored.getId().getUuid());
    assertEquals("profile text", restored.getPayloadOrThrow("text").getStringValue());
    assertEquals(List.of(0.25F, 0.75F), restored.getVectors().getVector().getDataList());
  }

  @Test
  void shouldFailSnapshotWhenQdrantCannotBeRead() throws Exception {
    VectorStore vectorStore = mock(VectorStore.class);
    QdrantClient client = mock(QdrantClient.class);
    SettableFuture<Points.ScrollResponse> scroll = SettableFuture.create();
    scroll.setException(new IllegalStateException("private backend details"));
    when(client.scrollAsync(any(Points.ScrollPoints.class), any(java.time.Duration.class)))
        .thenReturn(scroll);
    QdrantCandidateVectorRepository repository =
        new QdrantCandidateVectorRepository(
            vectorStore, client, new CandidateEmbeddingDocumentBuilder(), "candidate-resumes");

    ResumeIngestionException failure =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResumeIngestionException.class, () -> repository.snapshotByEmail("jane@example.com"));

    assertEquals("Qdrant candidate snapshot failed", failure.getMessage());
  }

  @Test
  void shouldContinueSnapshotAcrossQdrantPages() throws Exception {
    VectorStore vectorStore = mock(VectorStore.class);
    QdrantClient client = mock(QdrantClient.class);
    Points.RetrievedPoint point = pointWithDenseVector();
    SettableFuture<Points.ScrollResponse> firstPage = SettableFuture.create();
    firstPage.set(
        Points.ScrollResponse.newBuilder()
            .addResult(point)
            .setNextPageOffset(PointId.newBuilder().setUuid("next"))
            .build());
    SettableFuture<Points.ScrollResponse> secondPage = SettableFuture.create();
    secondPage.set(Points.ScrollResponse.getDefaultInstance());
    when(client.scrollAsync(any(Points.ScrollPoints.class), any(java.time.Duration.class)))
        .thenReturn(firstPage, secondPage);
    QdrantCandidateVectorRepository repository =
        new QdrantCandidateVectorRepository(
            vectorStore, client, new CandidateEmbeddingDocumentBuilder(), "candidate-resumes");

    assertEquals(1, repository.snapshotByEmail("jane@example.com").points().size());

    verify(client, org.mockito.Mockito.times(2))
        .scrollAsync(any(Points.ScrollPoints.class), any(java.time.Duration.class));
  }

  @Test
  void shouldRestoreEmptySnapshotByDeletingCurrentPointsOnly() {
    VectorStore vectorStore = mock(VectorStore.class);
    QdrantClient client = mock(QdrantClient.class);
    QdrantCandidateVectorRepository repository =
        new QdrantCandidateVectorRepository(
            vectorStore, client, new CandidateEmbeddingDocumentBuilder(), "candidate-resumes");

    repository.restoreByEmail("jane@example.com", CandidateVectorSnapshot.empty());

    verify(vectorStore).delete(any(Expression.class));
    verify(client, org.mockito.Mockito.never())
        .upsertAsync(any(String.class), any(List.class), any(java.time.Duration.class));
  }

  @Test
  void shouldSanitizeRestoreAndIndexFailures() throws Exception {
    VectorStore vectorStore = mock(VectorStore.class);
    QdrantClient client = mock(QdrantClient.class);
    SettableFuture<UpdateResult> failed = SettableFuture.create();
    failed.setException(new IllegalStateException("backend details"));
    when(client.upsertAsync(
            eq("candidate-resumes"), any(List.class), any(java.time.Duration.class)))
        .thenReturn(failed);
    when(client.createPayloadIndexAsync(
            eq("candidate-resumes"),
            eq("email"),
            eq(PayloadSchemaType.Keyword),
            eq(null),
            eq(true),
            eq(null),
            any(java.time.Duration.class)))
        .thenReturn(failed);
    QdrantCandidateVectorRepository repository =
        new QdrantCandidateVectorRepository(
            vectorStore, client, new CandidateEmbeddingDocumentBuilder(), "candidate-resumes");

    assertEquals(
        "Qdrant candidate restore failed",
        org.junit.jupiter.api.Assertions.assertThrows(
                ResumeIngestionException.class,
                () ->
                    repository.restoreByEmail(
                        "jane@example.com",
                        new CandidateVectorSnapshot(
                            List.of(
                                new CandidateVectorPoint(
                                    "point-id", java.util.Map.of(), java.util.Map.of())))))
            .getMessage());
    assertEquals(
        "Qdrant email index setup failed",
        org.junit.jupiter.api.Assertions.assertThrows(
                ResumeIngestionException.class, repository::ensureEmailPayloadIndex)
            .getMessage());
  }

  @Test
  void shouldRejectSnapshotWhenQdrantReturnsUnsupportedVector() throws Exception {
    VectorStore vectorStore = mock(VectorStore.class);
    QdrantClient client = mock(QdrantClient.class);
    Points.RetrievedPoint point =
        Points.RetrievedPoint.newBuilder()
            .setId(PointId.newBuilder().setUuid("point-uuid"))
            .setVectors(
                Points.VectorsOutput.newBuilder()
                    .setVector(Points.VectorOutput.getDefaultInstance()))
            .build();
    SettableFuture<Points.ScrollResponse> scroll = SettableFuture.create();
    scroll.set(Points.ScrollResponse.newBuilder().addResult(point).build());
    when(client.scrollAsync(any(Points.ScrollPoints.class), any(java.time.Duration.class)))
        .thenReturn(scroll);
    QdrantCandidateVectorRepository repository =
        new QdrantCandidateVectorRepository(
            vectorStore, client, new CandidateEmbeddingDocumentBuilder(), "candidate-resumes");

    assertEquals(
        "Qdrant returned an unsupported vector snapshot",
        org.junit.jupiter.api.Assertions.assertThrows(
                ResumeIngestionException.class,
                () -> repository.snapshotByEmail("jane@example.com"))
            .getMessage());
  }

  @Test
  void shouldSnapshotNamedDenseVectors() throws Exception {
    VectorStore vectorStore = mock(VectorStore.class);
    QdrantClient client = mock(QdrantClient.class);
    Points.VectorOutput namedVector =
        Points.VectorOutput.newBuilder()
            .setDense(Points.DenseVector.newBuilder().addData(0.5F))
            .build();
    Points.RetrievedPoint point =
        Points.RetrievedPoint.newBuilder()
            .setId(PointId.newBuilder().setUuid("point-uuid"))
            .setVectors(
                Points.VectorsOutput.newBuilder()
                    .setVectors(
                        Points.NamedVectorsOutput.newBuilder().putVectors("profile", namedVector)))
            .build();
    SettableFuture<Points.ScrollResponse> scroll = SettableFuture.create();
    scroll.set(Points.ScrollResponse.newBuilder().addResult(point).build());
    when(client.scrollAsync(any(Points.ScrollPoints.class), any(java.time.Duration.class)))
        .thenReturn(scroll);
    QdrantCandidateVectorRepository repository =
        new QdrantCandidateVectorRepository(
            vectorStore, client, new CandidateEmbeddingDocumentBuilder(), "candidate-resumes");

    CandidateVectorPoint snapshot =
        repository.snapshotByEmail("jane@example.com").points().getFirst();

    assertEquals(List.of(0.5F), snapshot.vectors().get("profile"));
  }

  @Test
  void shouldRestoreInterruptStatusWhenRestoreIsInterrupted() {
    VectorStore vectorStore = mock(VectorStore.class);
    QdrantClient client = mock(QdrantClient.class);
    QdrantCandidateVectorRepository repository =
        new QdrantCandidateVectorRepository(
            vectorStore, client, new CandidateEmbeddingDocumentBuilder(), "candidate-resumes");
    SettableFuture<UpdateResult> upsert = SettableFuture.create();
    upsert.set(UpdateResult.getDefaultInstance());
    when(client.upsertAsync(
            eq("candidate-resumes"), any(List.class), any(java.time.Duration.class)))
        .thenReturn(upsert);
    Thread.currentThread().interrupt();

    try {
      assertEquals(
          "Qdrant candidate restore failed",
          org.junit.jupiter.api.Assertions.assertThrows(
                  ResumeIngestionException.class,
                  () ->
                      repository.restoreByEmail(
                          "jane@example.com",
                          new CandidateVectorSnapshot(
                              List.of(
                                  new CandidateVectorPoint(
                                      "point-id", java.util.Map.of(), java.util.Map.of())))))
              .getMessage());
      org.junit.jupiter.api.Assertions.assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void shouldRestoreInterruptStatusWhenIndexSetupIsInterrupted() {
    VectorStore vectorStore = mock(VectorStore.class);
    QdrantClient client = mock(QdrantClient.class);
    QdrantCandidateVectorRepository repository =
        new QdrantCandidateVectorRepository(
            vectorStore, client, new CandidateEmbeddingDocumentBuilder(), "candidate-resumes");
    SettableFuture<UpdateResult> index = SettableFuture.create();
    index.set(UpdateResult.getDefaultInstance());
    when(client.createPayloadIndexAsync(
            eq("candidate-resumes"),
            eq("email"),
            eq(PayloadSchemaType.Keyword),
            eq(null),
            eq(true),
            eq(null),
            any(java.time.Duration.class)))
        .thenReturn(index);
    Thread.currentThread().interrupt();

    try {
      assertEquals(
          "Qdrant email index setup failed",
          org.junit.jupiter.api.Assertions.assertThrows(
                  ResumeIngestionException.class, repository::ensureEmailPayloadIndex)
              .getMessage());
      org.junit.jupiter.api.Assertions.assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }

  private Points.RetrievedPoint pointWithDenseVector() {
    return Points.RetrievedPoint.newBuilder()
        .setId(PointId.newBuilder().setUuid("point-uuid"))
        .putPayload(
            "email", JsonWithInt.Value.newBuilder().setStringValue("jane@example.com").build())
        .setVectors(
            Points.VectorsOutput.newBuilder()
                .setVector(
                    Points.VectorOutput.newBuilder()
                        .setDense(Points.DenseVector.newBuilder().addData(0.25F))))
        .build();
  }
}
