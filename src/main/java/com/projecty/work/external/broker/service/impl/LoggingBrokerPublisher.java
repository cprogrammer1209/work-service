package com.projecty.work.external.broker.service.impl;

import com.projecty.work.external.broker.dto.BrokerMessageDTO;
import com.projecty.work.external.broker.service.BrokerPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "projecty.broker.type", havingValue = "logging")
public class LoggingBrokerPublisher implements BrokerPublisher {

	private static final Logger log = LoggerFactory.getLogger(LoggingBrokerPublisher.class);

	@Override
	public void publish(BrokerMessageDTO message) {
		log.info("Business event topic={} key={} value={}", message.topic(), message.key(), message.value());
	}

}
