package com.saptarsidebnath.tools.scheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saptarsidebnath.tools.scheduler.models.DomainLookUpRequest;
import com.saptarsidebnath.tools.scheduler.models.JobState;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ReportService {
  private final Map<UUID, DomainLookUpRequest> dnsCachingByReqId = new ConcurrentHashMap<>();

  private ObjectMapper objectMapper;

  @SneakyThrows
  public DomainLookUpRequest startCaching(final String domain, final Long delayInSeconds) {
    UUID id = UUID.randomUUID();
    DomainLookUpRequest lookupReq =
        DomainLookUpRequest.builder()
            .domainName(domain)
            .id(id)
            .requestTime(Instant.now())
            .scheduledLookupTime(Instant.now().plusSeconds(delayInSeconds))
            .jobStatus(JobState.SCHEDULED)
            .build();
    log.info(objectMapper.writeValueAsString(lookupReq));
    return this.dnsCachingByReqId.put(id, lookupReq);
  }

  @SneakyThrows
  public void updateReport(final DomainLookUpRequest request) {
    log.info(objectMapper.writeValueAsString(request));
    this.dnsCachingByReqId.put(request.id(), request);
  }
}
