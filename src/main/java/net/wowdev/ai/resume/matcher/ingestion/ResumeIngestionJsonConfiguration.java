package net.wowdev.ai.resume.matcher.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ResumeIngestionJsonConfiguration {

  @Bean
  @ConditionalOnMissingBean(ObjectMapper.class)
  ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
