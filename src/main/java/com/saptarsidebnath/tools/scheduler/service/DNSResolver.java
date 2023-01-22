package com.saptarsidebnath.tools.scheduler.service;

import static com.saptarsidebnath.tools.scheduler.configurations.Constants.NIO_EVENT_GROUP_EXECUTORS;
import static com.saptarsidebnath.tools.scheduler.configurations.Constants.NO_DELAY_IN_SECONDS;
import static com.saptarsidebnath.tools.scheduler.configurations.Constants.SCHEDULED_EXECUTOR_SERVICE;

import com.saptarsidebnath.tools.scheduler.models.DnsServerConfig;
import com.saptarsidebnath.tools.scheduler.models.DomainLookUpRequest;
import com.saptarsidebnath.tools.scheduler.models.DomainLookUpRequest.DomainLookUpRequestBuilder;
import com.saptarsidebnath.tools.scheduler.models.JobState;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.AbstractDnsRecord;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.resolver.dns.SequentialDnsServerAddressStreamProvider;
import io.netty.util.concurrent.Future;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DNSResolver {

  private final ReportService reportService;
  private final DnsServerConfig dnsConfig;

  private final NioEventLoopGroup nioEventLoopGroup;

  private final List<DnsRecordType> dnsRecordTypes;

  private final ScheduledExecutorService scheduledExecutorService;

  @Autowired
  public DNSResolver(
      ReportService reportService,
      DnsServerConfig dnsConfig,
      @Qualifier(NIO_EVENT_GROUP_EXECUTORS) NioEventLoopGroup nioEventLoopGroup,
      List<DnsRecordType> dnsRecordTypes,
      @Qualifier(SCHEDULED_EXECUTOR_SERVICE) ScheduledExecutorService scheduledExecutorService) {
    this.reportService = reportService;
    this.dnsConfig = dnsConfig;
    this.nioEventLoopGroup = nioEventLoopGroup;
    this.dnsRecordTypes = dnsRecordTypes;
    this.scheduledExecutorService = scheduledExecutorService;
  }

  private void queryDNS(final DnsQuestion dnsQuestions, final DomainLookUpRequest reportData) {
    this.reportService.updateReport(
        reportData.toBuilder().jobStatus(JobState.RUNNING).startTime(Instant.now()).build());
    final DnsNameResolverBuilder dnsNameResolverBuilder =
        new DnsNameResolverBuilder(this.nioEventLoopGroup.next())
            .channelType(NioDatagramChannel.class)
            .queryTimeoutMillis(dnsConfig.queryTimeOut().toMillis())
            .nameServerProvider(
                new SequentialDnsServerAddressStreamProvider(dnsConfig.dnsAddress()));
    Future<List<DnsRecord>> listFuture = dnsNameResolverBuilder.build().resolveAll(dnsQuestions);
    listFuture.addListener(
        future -> {
          DomainLookUpRequestBuilder domainLookUpRequestBuilder =
              reportData.toBuilder()
                  .jobStatus(future.isSuccess() ? JobState.COMPLETED : JobState.ERROR)
                  .completionTime(Instant.now());

          if (future.isSuccess()) {
            List<DefaultDnsRawRecord> defaultDnsRawRecords =
                (List<DefaultDnsRawRecord>) future.get();
            domainLookUpRequestBuilder
                .jobStatus(JobState.COMPLETED)
                .dnsRecord(defaultDnsRawRecords);
            Long delayBeforeReCaching =
                defaultDnsRawRecords.stream()
                    .map(AbstractDnsRecord::timeToLive)
                    .min(Long::compareTo)
                    .map(ttl -> (ttl * this.dnsConfig.pctOfDnsTTLToWait() / 100.0))
                    .map(Math::floor)
                    .map(Double::longValue)
                    .orElse(NO_DELAY_IN_SECONDS);
            this.resolveDNS(dnsQuestions.name(), delayBeforeReCaching);
          } else {
            domainLookUpRequestBuilder.jobStatus(JobState.ERROR);
          }

          this.reportService.updateReport(domainLookUpRequestBuilder.build());
        });
  }

  public void resolveDNS(final String domainName) {
    this.resolveDNS(domainName, NO_DELAY_IN_SECONDS);
  }

  public void resolveDNS(final String domainName, final Long delayInSeconds) {
    DomainLookUpRequest reportData = reportService.startCaching(domainName, delayInSeconds);
    scheduledExecutorService.schedule(
        () ->
            dnsRecordTypes.forEach(
                recordType -> queryDNS(new DefaultDnsQuestion(domainName, recordType), reportData)),
        delayInSeconds,
        TimeUnit.SECONDS);
  }
}
