package net.wowdev.ai.resume.matcher.matching;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.wowdev.ai.resume.matcher.model.Candidate;
import net.wowdev.ai.resume.matcher.model.CandidateExperience;
import net.wowdev.ai.resume.matcher.model.JobOffer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class CandidateMatchPromptBuilder {

  private final ObjectMapper objectMapper;
  private final String instructions;

  public CandidateMatchPromptBuilder(
      ObjectMapper objectMapper,
      @Value("classpath:prompts/rank-candidates.st") Resource promptResource) {
    this.objectMapper = objectMapper;
    try {
      this.instructions = promptResource.getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot load candidate ranking prompt");
    }
  }

  public String build(JobOffer position, List<Candidate> candidates) {
    ObjectNode context = objectMapper.createObjectNode();
    context.set("position", positionNode(position));
    context.set("candidates", candidateNodes(candidates));
    try {
      return instructions + "\n\nRanking input:\n" + objectMapper.writeValueAsString(context);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize candidate ranking input");
    }
  }

  private ObjectNode positionNode(JobOffer position) {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("jobTitle", valueOrEmpty(position.jobTitle()));
    node.put("company", valueOrEmpty(position.company()));
    node.put("jobDescription", valueOrEmpty(position.jobDescription()));
    node.put("requiredQualifications", valueOrEmpty(position.requiredQualifications()));
    if (position.yearsOfExperience() == null) {
      node.putNull("yearsOfExperience");
    } else {
      node.put("yearsOfExperience", position.yearsOfExperience());
    }
    node.put("location", valueOrEmpty(position.location()));
    node.put("allocation", valueOrEmpty(position.allocation()));
    return node;
  }

  private ArrayNode candidateNodes(List<Candidate> candidates) {
    ArrayNode nodes = objectMapper.createArrayNode();
    for (Candidate candidate : candidates) {
      nodes.add(candidateNode(candidate));
    }
    return nodes;
  }

  private ObjectNode candidateNode(Candidate candidate) {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("candidateId", valueOrEmpty(candidate.id()));
    node.put("fullname", valueOrEmpty(candidate.fullname()));
    node.put("country", valueOrEmpty(candidate.country()));
    node.put("city", valueOrEmpty(candidate.city()));
    node.put("position", valueOrEmpty(candidate.position()));
    node.put("skills", valueOrEmpty(candidate.skills()));
    node.put("allocation", valueOrEmpty(candidate.allocation()));
    node.put("yearsOfExperience", valueOrEmpty(candidate.yearsOfExperience()));
    ArrayNode experiences = objectMapper.createArrayNode();
    for (CandidateExperience experience : candidate.experiences()) {
      experiences.add(experienceNode(experience));
    }
    node.set("experiences", experiences);
    return node;
  }

  private ObjectNode experienceNode(CandidateExperience experience) {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("title", valueOrEmpty(experience.title()));
    node.put("company", valueOrEmpty(experience.company()));
    node.put("period", valueOrEmpty(experience.period()));
    node.put("responsibilities", valueOrEmpty(experience.responsibilities()));
    node.put("achievements", valueOrEmpty(experience.achievements()));
    return node;
  }

  private String valueOrEmpty(String value) {
    return value == null ? "" : value;
  }
}
