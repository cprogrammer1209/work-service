package com.projecty.work.external.broker.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Creates the configured topics at startup (existing ones are left alone).
 */
@Configuration
@ConditionalOnProperty(name = "projecty.broker.type", havingValue = "kafka", matchIfMissing = true)
public class KafkaTopicsConfig {

	@Bean
	public KafkaAdmin.NewTopics workTopics(BrokerProperties properties) {
		NewTopic[] topics = properties.topics()
			.stream()
			.map((name) -> TopicBuilder.name(name).partitions(properties.partitions()).replicas(1).build())
			.toArray(NewTopic[]::new);
		return new KafkaAdmin.NewTopics(topics);
	}

}
