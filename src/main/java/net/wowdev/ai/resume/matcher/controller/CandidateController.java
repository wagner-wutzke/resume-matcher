package net.wowdev.ai.resume.matcher.controller;

import net.wowdev.ai.resume.matcher.dto.CandidateDetailsResponse;
import net.wowdev.ai.resume.matcher.dto.CandidatePageResponse;
import net.wowdev.ai.resume.matcher.service.CandidateQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

  private final CandidateQueryService service;

  public CandidateController(CandidateQueryService service) {
    this.service = service;
  }

  @GetMapping
  public CandidatePageResponse findPage(@RequestParam(defaultValue = "0") Integer page) {
    return service.findPage(page);
  }

  @GetMapping("/{id}")
  public CandidateDetailsResponse findDetails(@PathVariable String id) {
    return service.findDetails(id);
  }
}
