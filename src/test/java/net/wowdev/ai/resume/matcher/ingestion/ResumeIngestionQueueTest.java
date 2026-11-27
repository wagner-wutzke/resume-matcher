package net.wowdev.ai.resume.matcher.ingestion;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ResumeIngestionQueueTest {

  @Test
  void shouldLogDiscoveryAndQueueWithFilenameAndStage() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger(ResumeIngestionQueue.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    CountDownLatch processed = new CountDownLatch(1);
    ResumeIngestionQueue queue = new ResumeIngestionQueue(path -> processed.countDown());

    try {
      queue.enqueue(Path.of("good.pdf"));
      assertTrue(processed.await(5, TimeUnit.SECONDS));

      String logs =
          appender.list.stream()
              .map(ILoggingEvent::getFormattedMessage)
              .reduce("", (left, right) -> left + "\n" + right);
      assertTrue(logs.contains("file=good.pdf stage=discovery outcome=discovered"));
      assertTrue(logs.contains("file=good.pdf stage=queue outcome=queued"));
    } finally {
      queue.close();
      logger.detachAppender(appender);
      appender.stop();
    }
  }
}
