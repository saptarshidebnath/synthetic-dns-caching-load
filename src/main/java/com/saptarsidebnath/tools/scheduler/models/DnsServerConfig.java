package com.saptarsidebnath.tools.scheduler.models;

import java.net.InetSocketAddress;
import java.time.Duration;
import lombok.Builder;

@Builder
public record DnsServerConfig(
    InetSocketAddress dnsAddress, Duration queryTimeOut, Double pctOfDnsTTLToWait) {}
