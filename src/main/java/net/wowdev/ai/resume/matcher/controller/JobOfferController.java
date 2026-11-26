package net.wowdev.ai.resume.matcher.controller;

import java.net.URI;
import java.util.List;
import net.wowdev.ai.resume.matcher.dto.JobOfferRequest;
import net.wowdev.ai.resume.matcher.dto.JobOfferResponse;
import net.wowdev.ai.resume.matcher.service.JobOfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-offers")
public class JobOfferController {

  private final JobOfferService service;

  public JobOfferController(JobOfferService service) {
    this.service = service;
  }

  @GetMapping
  public List<JobOfferResponse> list() {
    return service.findAll().stream().map(JobOfferResponse::from).toList();
  }

  @PostMapping
  public ResponseEntity<JobOfferResponse> create(@RequestBody JobOfferRequest request) {
    var offer = service.create(request);
    var location = URI.create("/api/job-offers/" + offer.id());
    return ResponseEntity.created(location).body(JobOfferResponse.from(offer));
  }

  @PutMapping("/{id}")
  public JobOfferResponse update(@PathVariable String id, @RequestBody JobOfferRequest request) {
    return JobOfferResponse.from(service.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

}
