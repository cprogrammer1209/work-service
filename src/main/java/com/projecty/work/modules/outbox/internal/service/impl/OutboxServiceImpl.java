package com.projecty.work.modules.outbox.internal.service.impl;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.projecty.work.external.broker.dto.BrokerMessageDTO;
import com.projecty.work.external.broker.service.BrokerPublisher;
import com.projecty.work.modules.outbox.api.dto.OutboxEventCreateDTO;
import com.projecty.work.modules.outbox.internal.config.OutboxProperties;
import com.projecty.work.modules.outbox.internal.constants.OutboxConstants;
import com.projecty.work.modules.outbox.internal.entity.OutboxEvent;
import com.projecty.work.modules.outbox.internal.repository.OutboxEventRepository;
import com.projecty.work.modules.outbox.internal.service.OutboxService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class OutboxServiceImpl implements OutboxService {

	private static final Logger log = LoggerFactory.getLogger(OutboxServiceImpl.class);

	private final OutboxEventRepository repository;

	private final BrokerPublisher brokerPublisher;

	private final OutboxProperties properties;

	private final ObjectMapper objectMapper;

	private final Clock clock;

	public OutboxServiceImpl(OutboxEventRepository repository, BrokerPublisher brokerPublisher,
			OutboxProperties properties, ObjectMapper objectMapper, Clock clock) {
		this.repository = repository;
		this.brokerPublisher = brokerPublisher;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void publish(OutboxEventCreateDTO event) {
		String payload;
		try {
			payload = this.objectMapper.writeValueAsString(event.payload());
		}
		catch (JacksonException ex) {
			throw new IllegalArgumentException(OutboxConstants.PAYLOAD_SERIALIZATION_FAILED, ex);
		}
		this.repository.save(new OutboxEvent(event.portalId(), event.aggregateType(), event.aggregateId(),
				event.eventType(), OutboxConstants.CURRENT_VERSION, event.actorUserId(), event.actorName(), payload,
				this.clock.instant()));
	}

	@Override
	@Transactional
	public int relayBatch() {
		List<OutboxEvent> events = this.repository.lockOldestUnpublished(this.properties.batchSize());
		int published = 0;
		for (OutboxEvent event : events) {
			try {
				this.brokerPublisher.publish(toMessage(event));
			}
			catch (RuntimeException ex) {
				log.warn("Outbox event {} ({}) not published, will retry: {}", event.getId(), event.getEventType(),
						ex.getMessage());
				break;
			}
			event.markPublished(this.clock.instant());
			published++;
		}
		return published;
	}

	@Override
	@Transactional
	public int purgePublished() {
		return this.repository.deletePublishedBefore(this.clock.instant().minus(this.properties.retention()));
	}

	/**
	 * The envelope every consumer receives: the same fields for every event, with the event's own data under
	 * {@code payload}. The key is the aggregate id, so events of one item stay in order.
	 */
	private BrokerMessageDTO toMessage(OutboxEvent event) {
		ObjectNode envelope = this.objectMapper.createObjectNode();
		envelope.put(OutboxConstants.ENVELOPE_EVENT_ID, event.getId().toString());
		envelope.put(OutboxConstants.ENVELOPE_TYPE, event.getEventType());
		envelope.put(OutboxConstants.ENVELOPE_VERSION, event.getVersion());
		envelope.put(OutboxConstants.ENVELOPE_OCCURRED_AT, event.getCreatedAt().toString());
		envelope.put(OutboxConstants.ENVELOPE_PORTAL_ID, (event.getPortalId() != null) ? event.getPortalId().toString() : null);
		envelope.put(OutboxConstants.ENVELOPE_AGGREGATE_TYPE, event.getAggregateType());
		envelope.put(OutboxConstants.ENVELOPE_AGGREGATE_ID, event.getAggregateId().toString());
		if (event.getActorUserId() != null) {
			ObjectNode actor = envelope.putObject(OutboxConstants.ENVELOPE_ACTOR);
			actor.put(OutboxConstants.ENVELOPE_ACTOR_USER_ID, event.getActorUserId().toString());
			actor.put(OutboxConstants.ENVELOPE_ACTOR_NAME, event.getActorName());
		}
		envelope.set(OutboxConstants.ENVELOPE_PAYLOAD, this.objectMapper.readTree(event.getPayload()));

		Map<String, String> headers = new HashMap<>();
		headers.put(OutboxConstants.HEADER_EVENT_ID, event.getId().toString());
		headers.put(OutboxConstants.HEADER_EVENT_TYPE, event.getEventType());
		headers.put(OutboxConstants.HEADER_VERSION, String.valueOf(event.getVersion()));
		if (event.getPortalId() != null) {
			headers.put(OutboxConstants.HEADER_PORTAL_ID, event.getPortalId().toString());
		}
		return new BrokerMessageDTO(OutboxConstants.TOPIC_PREFIX + event.getAggregateType().toLowerCase(),
				event.getAggregateId().toString(), this.objectMapper.writeValueAsString(envelope), headers);
	}

}
