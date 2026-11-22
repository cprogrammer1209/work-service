package com.projecty.work.external.broker.service;

import com.projecty.work.external.broker.dto.BrokerMessageDTO;

/**
 * Hands a business event to the message broker (Kafka; {@code projecty.broker.type=logging} only logs, for
 * tests).
 */
public interface BrokerPublisher {

	/**
	 * Returns only after the broker accepted the message.
	 * @throws RuntimeException when it did not; the caller then retries
	 */
	void publish(BrokerMessageDTO message);

}
