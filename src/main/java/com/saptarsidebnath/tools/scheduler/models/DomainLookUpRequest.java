package com.saptarsidebnath.tools.scheduler.models;

import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record DomainLookUpRequest(
    UUID id,
    String domainName,
    Instant requestTime,
    Instant scheduledLookupTime,
    Instant startTime,
    Instant completionTime,
    JobState jobStatus,
    List<DefaultDnsRawRecord> dnsRecord) {}
