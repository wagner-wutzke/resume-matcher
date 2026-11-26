package net.wowdev.ai.resume.matcher.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobOfferErrorResponse(
    String code,
    String message,
    Map<String, String> fieldErrors) {
}
