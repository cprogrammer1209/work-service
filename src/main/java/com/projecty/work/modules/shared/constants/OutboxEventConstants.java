package com.projecty.work.modules.shared.constants;

/**
 * Business event and aggregate names Work publishes through the outbox. The topic is {@code projecty.} plus the
 * lower-cased aggregate, so PROJECT events go to {@code projecty.project} (listed in projecty.broker.topics).
 */
public final class OutboxEventConstants {

	public static final String AGGREGATE_PROJECT = "PROJECT";

	public static final String EVENT_PROJECT_CREATED = "ProjectCreated";

	public static final String EVENT_PROJECT_UPDATED = "ProjectUpdated";

	public static final String EVENT_PROJECT_STATUS_CHANGED = "ProjectStatusChanged";

	public static final String EVENT_PROJECT_TRASHED = "ProjectTrashed";

	public static final String EVENT_PROJECT_RESTORED = "ProjectRestored";

	public static final String EVENT_PROJECT_MEMBER_ADDED = "ProjectMemberAdded";

	public static final String EVENT_PROJECT_MEMBER_REMOVED = "ProjectMemberRemoved";

	private OutboxEventConstants() {
	}

}
