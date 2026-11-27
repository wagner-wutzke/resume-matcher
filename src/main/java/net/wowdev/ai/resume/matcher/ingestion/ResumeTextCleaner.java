package net.wowdev.ai.resume.matcher.ingestion;

import org.springframework.stereotype.Component;

@Component
public class ResumeTextCleaner {

  public String clean(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    return text.replace("\r\n", "\n")
        .replace('\r', '\n')
        .replaceAll("[^\\S\\n]+", " ")
        .replaceAll("\\n{3,}", "\n\n")
        .strip();
  }
}
