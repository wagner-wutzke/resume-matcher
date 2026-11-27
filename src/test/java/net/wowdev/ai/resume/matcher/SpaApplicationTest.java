package net.wowdev.ai.resume.matcher;

import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.wowdev.ai.resume.matcher.repository.JobOfferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "resume.ingestion.enabled=false",
      "resume.ingestion.stability-timeout=PT5S",
      "spring.ai.vectorstore.type=none"
    })
@AutoConfigureMockMvc
class SpaApplicationTest {

  @DynamicPropertySource
  static void configureIngestionInputDirectory(DynamicPropertyRegistry registry) {
    registry.add("resume.ingestion.input-directory", SpaApplicationTest::createInputDirectory);
  }

  private static String createInputDirectory() {
    try {
      return java.nio.file.Files.createTempDirectory(
              java.nio.file.Path.of("target"), "resume-inbox-")
          .toString();
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Cannot create test resume input directory");
    }
  }

  @Autowired private MockMvc mockMvc;

  @MockitoBean private JobOfferRepository repository;

  @BeforeEach
  void resetRepository() {
    reset(repository);
  }

  @Test
  void mapsRootToSpaAndServesApplicationIdentityAndTabs() throws Exception {
    mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));

    mockMvc
        .perform(get("/index.html"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Resume Matcher")))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Candidates Matching made easy")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Open Positions")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Candidates")));
  }

  @Test
  void candidateTabHasEmptyListAndDetailsRegions() throws Exception {
    mockMvc
        .perform(get("/index.html"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "class=\"candidate-list\" aria-live=\"polite\"></div>")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "aria-label=\"Selected candidate details\"></section>")));
  }
}
