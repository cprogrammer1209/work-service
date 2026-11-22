package com.projecty.work.modules.outbox.internal.constants;

public final class OutboxConstants {

	public static final String PAYLOAD_SERIALIZATION_FAILED = "Outbox payload could not be serialized";

	/**
	 * One topic per aggregate type: {@code projecty.portal}, {@code projecty.member}, ...
	 */
	public static final String TOPIC_PREFIX = "projecty.";

	public static final String ENVELOPE_EVENT_ID = "eventId";

	public static final String ENVELOPE_TYPE = "type";

	public static final String ENVELOPE_VERSION = "version";

	public static final String ENVELOPE_OCCURRED_AT = "occurredAt";

	public static final String ENVELOPE_PORTAL_ID = "portalId";

	public static final String ENVELOPE_AGGREGATE_TYPE = "aggregateType";

	public static final String ENVELOPE_AGGREGATE_ID = "aggregateId";

	public static final String ENVELOPE_ACTOR = "actor";

	public static final String ENVELOPE_ACTOR_USER_ID = "userId";

	public static final String ENVELOPE_ACTOR_NAME = "name";

	public static final String ENVELOPE_PAYLOAD = "payload";

	public static final String HEADER_EVENT_ID = "eventId";

	public static final String HEADER_EVENT_TYPE = "eventType";

	public static final String HEADER_VERSION = "version";

	public static final String HEADER_PORTAL_ID = "portalId";

	/**
	 * Version of every event written so far. A breaking change to a payload bumps it for that event type.
	 */
	public static final short CURRENT_VERSION = 1;

	private OutboxConstants() {
	}

}
