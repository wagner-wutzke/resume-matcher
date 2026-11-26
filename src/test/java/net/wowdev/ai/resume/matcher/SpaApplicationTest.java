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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SpaApplicationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private JobOfferRepository repository;

  @BeforeEach
  void resetRepository() {
    reset(repository);
  }

  @Test
  void mapsRootToSpaAndServesApplicationIdentityAndTabs() throws Exception {
    mockMvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("index.html"));

    mockMvc.perform(get("/index.html"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Resume Matcher")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Candidates Matching made easy")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Open Positions")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Candidates")));
  }

  @Test
  void candidateTabHasEmptyListAndDetailsRegions() throws Exception {
    mockMvc.perform(get("/index.html"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString(
            "class=\"candidate-list\" aria-live=\"polite\"></div>")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString(
            "aria-label=\"Selected candidate details\"></section>")));
  }
}
