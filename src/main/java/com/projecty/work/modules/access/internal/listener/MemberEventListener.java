package com.projecty.work.modules.access.internal.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.projecty.work.modules.access.internal.constants.AccessConstants;
import com.projecty.work.modules.access.internal.dto.MemberEventDTO;
import com.projecty.work.modules.access.internal.service.MemberAccessService;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads Platform's member events and hands them to the access service. Unknown fields and event types are ignored,
 * as the event contract (docs/07-events.md) asks of consumers.
 */
@Component
public class MemberEventListener {

	private final ObjectMapper objectMapper;

	private final MemberAccessService accessService;

	public MemberEventListener(ObjectMapper objectMapper, MemberAccessService accessService) {
		this.objectMapper = objectMapper;
		this.accessService = accessService;
	}

	@KafkaListener(topics = AccessConstants.TOPIC_MEMBER, groupId = AccessConstants.GROUP_ID)
	public void onMessage(String message) {
		JsonNode envelope = this.objectMapper.readTree(message);
		JsonNode payload = envelope.path(AccessConstants.FIELD_PAYLOAD);
		List<String> permissions = null;
		if (payload.has(AccessConstants.FIELD_PERMISSIONS)) {
			permissions = new ArrayList<>();
			for (JsonNode name : payload.get(AccessConstants.FIELD_PERMISSIONS)) {
				permissions.add(name.asString());
			}
		}
		this.accessService.apply(new MemberEventDTO(UUID.fromString(envelope.get(AccessConstants.FIELD_EVENT_ID).asString()),
				envelope.get(AccessConstants.FIELD_TYPE).asString(),
				UUID.fromString(payload.get(AccessConstants.FIELD_PORTAL_ID).asString()),
				UUID.fromString(payload.get(AccessConstants.FIELD_USER_ID).asString()),
				payload.has(AccessConstants.FIELD_PROFILE_KEY) ? payload.get(AccessConstants.FIELD_PROFILE_KEY).asString() : null,
				permissions));
	}

}
