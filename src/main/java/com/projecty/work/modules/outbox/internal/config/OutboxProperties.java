package com.projecty.work.modules.outbox.internal.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param pollInterval pause between relay runs
 * @param batchSize events published per run
 * @param relayEnabled false turns the relay off (events then stay in the table)
 * @param retention how long published events are kept before they are deleted
 * @param purgeInterval pause between clean-up runs
 */
@ConfigurationProperties("projecty.outbox")
public record OutboxProperties(Duration pollInterval, int batchSize, boolean relayEnabled, Duration retention,
		Duration purgeInterval) {
}
