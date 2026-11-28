package net.wowdev.ai.resume.matcher.controller;

import net.wowdev.ai.resume.matcher.dto.CandidateMatchResponse;
import net.wowdev.ai.resume.matcher.matching.CandidateMatchService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-offers")
public class CandidateMatchController {

  private final CandidateMatchService service;

  public CandidateMatchController(CandidateMatchService service) {
    this.service = service;
  }

  @PostMapping("/{positionId}/matches")
  public CandidateMatchResponse findMatches(@PathVariable String positionId) {
    return service.findMatches(positionId);
  }
}
