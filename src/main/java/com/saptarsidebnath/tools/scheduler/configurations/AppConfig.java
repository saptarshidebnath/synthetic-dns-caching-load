package com.saptarsidebnath.tools.scheduler.configurations;

import static com.saptarsidebnath.tools.scheduler.configurations.Constants.NIO_EVENT_GROUP_EXECUTORS;
import static com.saptarsidebnath.tools.scheduler.configurations.Constants.SCHEDULED_EXECUTOR_SERVICE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saptarsidebnath.tools.scheduler.models.DnsServerConfig;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.dns.DnsRecordType;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@Configuration
public class Configuration {

  private final Environment environment;

  public Configuration(Environment environment) {
    this.environment = environment;
  }

  @Bean
  public DnsServerConfig getDnsAddress() {
    return DnsServerConfig.builder()
        .dnsAddress(
            InetSocketAddress.createUnresolved(
                environment.getProperty("resolving-dns.ip"),
                environment.getProperty("resolving-dns.port", Integer.class).intValue()))
        .queryTimeOut(environment.getProperty("resolving-dns.queryTimeout", Duration.class))
        .pctOfDnsTTLToWait(
            environment.getProperty("resolving-dns.caching.pctOfDnsTTLToWait", Double.class))
        .build();
  }

  @Bean
  public ObjectMapper getObjectMapper() {
    return new ObjectMapper();
  }

  @Bean(name = NIO_EVENT_GROUP_EXECUTORS)
  public NioEventLoopGroup getNioEvenLoopGroup() {
    return new NioEventLoopGroup();
  }

  @Bean
  public List<DnsRecordType> getDnsRecordTypes() {
    return List.of(DnsRecordType.A, DnsRecordType.AAAA, DnsRecordType.CNAME);
  }

  @Bean(name = SCHEDULED_EXECUTOR_SERVICE)
  public ScheduledExecutorService getScheduledExecutorService() {
    return new ScheduledThreadPoolExecutor(1);
  }
}
