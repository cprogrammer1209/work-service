package com.projecty.work.external.broker.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param type {@code kafka} or {@code logging} (tests)
 * @param topics every topic this service publishes to; a new aggregate adds its topic here, because the broker
 * does not create topics on its own
 * @param partitions partitions per topic when created
 * @param sendTimeout how long the relay waits for the broker to accept one message
 */
@ConfigurationProperties("projecty.broker")
public record BrokerProperties(String type, List<String> topics, int partitions, Duration sendTimeout) {
}
