package net.wowdev.ai.resume.matcher.ingestion;

import static org.springframework.ai.vectorstore.filter.Filter.ExpressionType.EQ;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.PayloadSchemaType;
import io.qdrant.client.grpc.Common;
import io.qdrant.client.grpc.Common.PointId;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.PointStruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.CandidateVectorPoint;
import net.wowdev.ai.resume.matcher.model.CandidateVectorSnapshot;
import net.wowdev.ai.resume.matcher.repository.CandidateVectorRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.Filter.Key;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ResumeIngestionEnabled
@ConditionalOnProperty(name = "spring.ai.vectorstore.type", havingValue = "qdrant")
public class QdrantCandidateVectorRepository implements CandidateVectorRepository {

  private static final Duration QDRANT_TIMEOUT = Duration.ofSeconds(30);

  private final VectorStore vectorStore;
  private final QdrantClient qdrantClient;
  private final CandidateEmbeddingDocumentBuilder documentBuilder;
  private final String collectionName;

  public QdrantCandidateVectorRepository(
      VectorStore vectorStore,
      QdrantClient qdrantClient,
      CandidateEmbeddingDocumentBuilder documentBuilder,
      @org.springframework.beans.factory.annotation.Value(
              "${spring.ai.vectorstore.qdrant.collection-name:candidate-resumes}")
          String collectionName) {
    this.vectorStore = vectorStore;
    this.qdrantClient = qdrantClient;
    this.documentBuilder = documentBuilder;
    this.collectionName = collectionName;
  }

  @Override
  public void replaceByEmail(Candidate candidate, String sourceFileName) {
    deleteByEmail(candidate.email());
    List<Document> documents = documentBuilder.build(candidate, sourceFileName);
    vectorStore.add(documents);
  }

  @Override
  public void deleteByEmail(String email) {
    vectorStore.delete(emailFilter(email));
  }

  @Override
  public CandidateVectorSnapshot snapshotByEmail(String email) {
    List<CandidateVectorPoint> snapshot = new ArrayList<>();
    Points.ScrollPoints.Builder request =
        Points.ScrollPoints.newBuilder()
            .setCollectionName(collectionName)
            .setFilter(qdrantEmailFilter(email))
            .setLimit(256)
            .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true))
            .setWithVectors(Points.WithVectorsSelector.newBuilder().setEnable(true));
    try {
      while (true) {
        Points.ScrollResponse response =
            await(qdrantClient.scrollAsync(request.build(), QDRANT_TIMEOUT));
        response.getResultList().stream().map(this::toCandidatePoint).forEach(snapshot::add);
        if (!response.hasNextPageOffset() || response.getResultCount() == 0) {
          return new CandidateVectorSnapshot(snapshot);
        }
        request.setOffset(response.getNextPageOffset());
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw qdrantUnavailable("Qdrant candidate snapshot failed");
    } catch (ExecutionException | java.util.concurrent.TimeoutException exception) {
      throw qdrantUnavailable("Qdrant candidate snapshot failed");
    }
  }

  @Override
  public void restoreByEmail(String email, CandidateVectorSnapshot snapshot) {
    deleteByEmail(email);
    if (snapshot.points().isEmpty()) {
      return;
    }
    try {
      List<PointStruct> points = snapshot.points().stream().map(this::toPointStruct).toList();
      await(qdrantClient.upsertAsync(collectionName, points, QDRANT_TIMEOUT));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw rollbackFailed("Qdrant candidate restore failed");
    } catch (ExecutionException | java.util.concurrent.TimeoutException exception) {
      throw rollbackFailed("Qdrant candidate restore failed");
    }
  }

  public void ensureEmailPayloadIndex() {
    try {
      qdrantClient
          .createPayloadIndexAsync(
              collectionName, "email", PayloadSchemaType.Keyword, null, true, null, QDRANT_TIMEOUT)
          .get(QDRANT_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw qdrantUnavailable("Qdrant email index setup failed");
    } catch (ExecutionException | java.util.concurrent.TimeoutException exception) {
      throw qdrantUnavailable("Qdrant email index setup failed");
    }
  }

  private Expression emailFilter(String email) {
    return new Expression(
        EQ,
        new Key("email"),
        new org.springframework.ai.vectorstore.filter.Filter.Value(
            Candidate.normalizeEmail(email)));
  }

  private Common.Filter qdrantEmailFilter(String email) {
    Common.FieldCondition fieldCondition =
        Common.FieldCondition.newBuilder()
            .setKey("email")
            .setMatch(Common.Match.newBuilder().setKeyword(Candidate.normalizeEmail(email)))
            .build();
    return Common.Filter.newBuilder()
        .addMust(Common.Condition.newBuilder().setField(fieldCondition))
        .build();
  }

  private CandidateVectorPoint toCandidatePoint(Points.RetrievedPoint point) {
    return new CandidateVectorPoint(
        point.getId().getUuid(), toVectors(point.getVectors()), toPayload(point.getPayloadMap()));
  }

  private Map<String, List<Float>> toVectors(Points.VectorsOutput output) {
    Map<String, List<Float>> vectors = new HashMap<>();
    if (output.getVectorsOptionsCase() == Points.VectorsOutput.VectorsOptionsCase.VECTOR) {
      vectors.put("", toVector(output.getVector()));
      return Map.copyOf(vectors);
    }
    if (output.getVectorsOptionsCase() == Points.VectorsOutput.VectorsOptionsCase.VECTORS) {
      output
          .getVectors()
          .getVectorsMap()
          .forEach((name, vector) -> vectors.put(name, toVector(vector)));
      return Map.copyOf(vectors);
    }
    throw qdrantUnavailable("Qdrant returned an unsupported vector snapshot");
  }

  private List<Float> toVector(Points.VectorOutput output) {
    if (output.getVectorCase() != Points.VectorOutput.VectorCase.DENSE) {
      throw qdrantUnavailable("Qdrant returned an unsupported vector snapshot");
    }
    return List.copyOf(output.getDense().getDataList());
  }

  private Map<String, String> toPayload(Map<String, JsonWithInt.Value> payload) {
    Map<String, String> values = new HashMap<>();
    payload.forEach(
        (key, value) -> {
          if (value.getKindCase() != JsonWithInt.Value.KindCase.STRING_VALUE) {
            throw qdrantUnavailable("Qdrant returned unsupported candidate metadata");
          }
          values.put(key, value.getStringValue());
        });
    return Map.copyOf(values);
  }

  private PointStruct toPointStruct(CandidateVectorPoint point) {
    PointStruct.Builder builder =
        PointStruct.newBuilder()
            .setId(PointId.newBuilder().setUuid(point.id()))
            .setVectors(toVectors(point.vectors()));
    point
        .payload()
        .forEach(
            (key, value) ->
                builder.putPayload(
                    key,
                    JsonWithInt.Value.newBuilder().setStringValue(value).build()));
    return builder.build();
  }

  private Points.Vectors toVectors(Map<String, List<Float>> vectors) {
    if (vectors.containsKey("")) {
      return Points.Vectors.newBuilder().setVector(vector(vectors.get(""))).build();
    }
    Points.NamedVectors.Builder named = Points.NamedVectors.newBuilder();
    vectors.forEach((name, values) -> named.putVectors(name, vector(values)));
    return Points.Vectors.newBuilder().setVectors(named).build();
  }

  private Points.Vector vector(List<Float> values) {
    return Points.Vector.newBuilder().addAllData(values).build();
  }

  private <T> T await(com.google.common.util.concurrent.ListenableFuture<T> future)
      throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {
    return future.get(QDRANT_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
  }

  private ResumeIngestionException qdrantUnavailable(String message) {
    return new ResumeIngestionException(
        message, ResumeIngestionException.FailureCause.QDRANT_UNAVAILABLE);
  }

  private ResumeIngestionException rollbackFailed(String message) {
    return new ResumeIngestionException(
        message, ResumeIngestionException.FailureCause.ROLLBACK_FAILED);
  }
}
