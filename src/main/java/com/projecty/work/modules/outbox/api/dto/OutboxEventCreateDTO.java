package com.projecty.work.modules.outbox.api.dto;

import java.util.Map;
import java.util.UUID;

/**
 * One business event. {@code eventType} and {@code aggregateType} take the values in
 * {@code modules.shared.constants.OutboxEventConstants}. Events of one aggregate are published in order.
 * @param portalId the tenant the event concerns, or null for events that belong to no portal
 * @param actorUserId who caused it, or null when the system did
 * @param payload ids and before/after values only; it is stored as JSON
 */
public record OutboxEventCreateDTO(UUID portalId, String aggregateType, UUID aggregateId, String eventType,
		UUID actorUserId, String actorName, Map<String, Object> payload) {
}
