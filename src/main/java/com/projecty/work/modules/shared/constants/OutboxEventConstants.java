package com.projecty.work.modules.shared.constants;

/**
 * Business event and aggregate names Work publishes through the outbox. The topic is {@code projecty.} plus the
 * lower-cased aggregate, so PROJECT events go to {@code projecty.project} (listed in projecty.broker.topics).
 */
public final class OutboxEventConstants {

	public static final String AGGREGATE_PROJECT = "PROJECT";

	public static final String AGGREGATE_FIELD = "FIELD";

	public static final String AGGREGATE_LAYOUT = "LAYOUT";

	public static final String AGGREGATE_TASK = "TASK";

	public static final String AGGREGATE_TASK_LIST = "TASKLIST";

	public static final String EVENT_PROJECT_CREATED = "ProjectCreated";

	public static final String EVENT_PROJECT_UPDATED = "ProjectUpdated";

	public static final String EVENT_PROJECT_STATUS_CHANGED = "ProjectStatusChanged";

	public static final String EVENT_PROJECT_TRASHED = "ProjectTrashed";

	public static final String EVENT_PROJECT_RESTORED = "ProjectRestored";

	public static final String EVENT_PROJECT_MEMBER_ADDED = "ProjectMemberAdded";

	public static final String EVENT_PROJECT_MEMBER_REMOVED = "ProjectMemberRemoved";

	public static final String EVENT_PROJECT_LAYOUT_CHANGED = "ProjectLayoutChanged";

	public static final String EVENT_TASK_CREATED = "TaskCreated";

	public static final String EVENT_TASK_UPDATED = "TaskUpdated";

	public static final String EVENT_TASK_STATUS_CHANGED = "TaskStatusChanged";

	public static final String EVENT_TASK_CLOSED = "TaskClosed";

	public static final String EVENT_TASK_REOPENED = "TaskReopened";

	public static final String EVENT_TASK_TRASHED = "TaskTrashed";

	public static final String EVENT_TASK_RESTORED = "TaskRestored";

	public static final String EVENT_TASK_COMMENT_ADDED = "TaskCommentAdded";

	public static final String EVENT_TASK_COMMENT_UPDATED = "TaskCommentUpdated";

	public static final String EVENT_TASK_COMMENT_DELETED = "TaskCommentDeleted";

	public static final String EVENT_TASK_LIST_CREATED = "TaskListCreated";

	public static final String EVENT_TASK_LIST_UPDATED = "TaskListUpdated";

	public static final String EVENT_TASK_LIST_ARCHIVED = "TaskListArchived";

	public static final String EVENT_LAYOUT_CREATED = "LayoutCreated";

	public static final String EVENT_LAYOUT_UPDATED = "LayoutUpdated";

	public static final String EVENT_FIELD_CREATED = "FieldCreated";

	public static final String EVENT_FIELD_UPDATED = "FieldUpdated";

	public static final String EVENT_FIELD_DEACTIVATED = "FieldDeactivated";

	public static final String EVENT_FIELD_DELETED = "FieldDeleted";

	private OutboxEventConstants() {
	}

}
