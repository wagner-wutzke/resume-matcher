package net.wowdev.ai.resume.matcher.matching;

import java.util.Set;

public interface CandidateSemanticRetriever {

  Set<String> retrieveCandidateEmails(String semanticQuery);
}
