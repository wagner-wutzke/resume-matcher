package net.wowdev.ai.resume.matcher.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "job_offer_sequences")
public record JobOfferSequence(@Id String id, long value) {
}
