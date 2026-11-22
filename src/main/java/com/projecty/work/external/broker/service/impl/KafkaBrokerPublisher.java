package com.projecty.work.external.broker.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.projecty.work.external.broker.config.BrokerProperties;
import com.projecty.work.external.broker.constants.BrokerConstants;
import com.projecty.work.external.broker.dto.BrokerMessageDTO;
import com.projecty.work.external.broker.service.BrokerPublisher;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "projecty.broker.type", havingValue = "kafka", matchIfMissing = true)
public class KafkaBrokerPublisher implements BrokerPublisher {

	private final KafkaTemplate<String, String> kafkaTemplate;

	private final BrokerProperties properties;

	public KafkaBrokerPublisher(KafkaTemplate<String, String> kafkaTemplate, BrokerProperties properties) {
		this.kafkaTemplate = kafkaTemplate;
		this.properties = properties;
	}

	@Override
	public void publish(BrokerMessageDTO message) {
		ProducerRecord<String, String> record = new ProducerRecord<>(message.topic(), message.key(),
				message.value());
		message.headers()
			.forEach((name, value) -> record.headers().add(name, value.getBytes(StandardCharsets.UTF_8)));
		try {
			this.kafkaTemplate.send(record).get(this.properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(BrokerConstants.PUBLISH_INTERRUPTED, ex);
		}
		catch (ExecutionException | TimeoutException ex) {
			throw new IllegalStateException(BrokerConstants.PUBLISH_FAILED + message.topic(), ex);
		}
	}

}
