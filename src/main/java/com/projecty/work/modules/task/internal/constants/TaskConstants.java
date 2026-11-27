package com.projecty.work.modules.task.internal.constants;

import java.time.Duration;

/**
 * Constants of the task module: limits, error codes, messages, detail keys and activity names.
 */
public final class TaskConstants {

	/** The module name task layouts and task custom fields are defined for (see the customfield module). */
	public static final String CUSTOM_FIELD_ENTITY = "TASK";

	public static final String SCOPE_MINE = "MINE";

	public static final String SCOPE_ALL = "ALL";

	public static final String STATUS_TYPE_OPEN = "OPEN";

	public static final int ACROSS_LIMIT_DEFAULT = 200;

	public static final int ACROSS_LIMIT_MAX = 500;

	public static final String DETAIL_SCOPE = "scope";

	public static final String DETAIL_STATUS_TYPE = "statusType";

	public static final String DETAIL_LIMIT = "limit";

	public static final String DETAIL_CURSOR = "cursor";

	public static final String MESSAGE_SCOPE_INVALID = "Use mine or all.";

	public static final String MESSAGE_STATUS_TYPE_INVALID = "Use OPEN or CLOSED.";

	public static final String MESSAGE_LIMIT_INVALID = "Use a whole number from 1 to " + ACROSS_LIMIT_MAX + ".";

	public static final String MESSAGE_CURSOR_INVALID = "That cursor is not valid. Use the nextCursor of a previous page.";

	public static final String PROFILE_ADMIN = "ADMIN";

	public static final String PROFILE_CLIENT = "CLIENT";

	public static final String STATUS_TYPE_CLOSED = "CLOSED";

	public static final String DEFAULT_LIST_NAME = "General";

	/** "WEB" + this + "42" gives the display id WEB-T42. */
	public static final String KEY_INFIX = "-T";

	public static final int TITLE_MAX_LENGTH = 200;

	public static final int DESCRIPTION_MAX_LENGTH = 10000;

	public static final int LIST_NAME_MAX_LENGTH = 100;

	public static final int COMMENT_MAX_LENGTH = 5000;

	public static final int TAG_LIMIT = 20;

	public static final int TAG_MAX_LENGTH = 30;

	public static final int OWNER_LIMIT = 20;

	/** A task and its subtasks go at most this many levels deep (a top-level task is level 1). */
	public static final int MAX_DEPTH = 5;

	public static final int COMPLETION_MAX = 100;

	/** NUMERIC(8,2): up to 999999.99 hours. */
	public static final int WORK_HOURS_MAX = 999999;

	public static final int WORK_HOURS_SCALE = 2;

	public static final int ACTIVITY_LIMIT = 100;

	public static final int ACTIVITY_VALUE_MAX_LENGTH = 500;

	public static final int ACTIVITY_DETAIL_MAX_LENGTH = 200;

	public static final String ACTION_CREATED = "TASK_CREATED";

	public static final String ACTION_UPDATED = "TASK_UPDATED";

	public static final String ACTION_STATUS_CHANGED = "TASK_STATUS_CHANGED";

	public static final String ACTION_TRASHED = "TASK_TRASHED";

	public static final String ACTION_RESTORED = "TASK_RESTORED";

	public static final String ACTION_COMMENT_ADDED = "TASK_COMMENT_ADDED";

	public static final String ACTION_COMMENT_EDITED = "TASK_COMMENT_EDITED";

	public static final String ACTION_COMMENT_DELETED = "TASK_COMMENT_DELETED";

	public static final String FIELD_TITLE = "title";

	public static final String FIELD_DESCRIPTION = "description";

	public static final String FIELD_PRIORITY = "priority";

	public static final String FIELD_START_DATE = "startDate";

	public static final String FIELD_DUE_DATE = "dueDate";

	public static final String FIELD_WORK_HOURS = "workHours";

	public static final String FIELD_COMPLETION = "completion";

	public static final String FIELD_OWNERS = "ownerIds";

	public static final String FIELD_TAGS = "tags";

	public static final String FIELD_FOLLOWERS = "followerIds";

	public static final String FIELD_STATUS = "statusId";

	public static final String FIELD_LIST = "listId";

	public static final String FIELD_PARENT = "parentId";

	public static final String FIELD_LAYOUT = "layoutId";

	public static final String FIELD_CUSTOM_PREFIX = "customValues.";

	public static final String ERROR_TASK_NOT_FOUND = "TASK_NOT_FOUND";

	public static final String ERROR_TASK_LIST_NOT_FOUND = "TASK_LIST_NOT_FOUND";

	public static final String ERROR_COMMENT_NOT_FOUND = "TASK_COMMENT_NOT_FOUND";

	public static final String ERROR_PROJECT_NOT_FOUND = "PROJECT_NOT_FOUND";

	public static final String ERROR_FORBIDDEN = "TASK_FORBIDDEN";

	public static final String ERROR_LIST_ARCHIVED = "TASK_LIST_ARCHIVED";

	public static final String ERROR_PARENT_TRASHED = "TASK_PARENT_TRASHED";

	public static final String ERROR_NOT_TRASHED = "TASK_NOT_TRASHED";

	public static final String ERROR_LAYOUT_INVALID = "TASK_LAYOUT_INVALID";

	public static final String MESSAGE_TASK_NOT_FOUND = "That task does not exist, or you cannot see it.";

	public static final String MESSAGE_TASK_LIST_NOT_FOUND = "That task list does not exist.";

	public static final String MESSAGE_COMMENT_NOT_FOUND = "That comment does not exist.";

	public static final String MESSAGE_PROJECT_NOT_FOUND = "That project does not exist, or you cannot see it.";

	public static final String MESSAGE_FORBIDDEN = "You don't have permission to do this.";

	public static final String MESSAGE_LIST_ARCHIVED = "That task list is archived. Choose another list.";

	public static final String MESSAGE_PARENT_TRASHED = "Restore the parent task first.";

	public static final String MESSAGE_NOT_TRASHED = "That task is not in the trash.";

	public static final String MESSAGE_LAYOUT_WRONG_MODULE = "That layout is not a task layout.";

	public static final String MESSAGE_LAYOUT_INACTIVE = "That layout is inactive. Choose an active layout.";

	public static final String MESSAGE_COMMENT_FORBIDDEN = "You can only change your own comments.";

	public static final String MESSAGE_TRASH_FORBIDDEN = "Only people who may delete tasks can see or restore the trash.";

	public static final String MESSAGE_FOLLOWER_FORBIDDEN = "You can only follow or unfollow a task yourself.";

	public static final String DETAIL_TITLE = "title";

	public static final String DETAIL_DESCRIPTION = "description";

	public static final String DETAIL_PRIORITY = "priority";

	public static final String DETAIL_DUE_DATE = "dueDate";

	public static final String DETAIL_WORK_HOURS = "workHours";

	public static final String DETAIL_COMPLETION = "completion";

	public static final String DETAIL_OWNERS = "ownerIds";

	public static final String DETAIL_TAGS = "tags";

	public static final String DETAIL_STATUS = "statusId";

	public static final String DETAIL_LIST = "listId";

	public static final String DETAIL_PARENT = "parentId";

	public static final String DETAIL_NAME = "name";

	public static final String DETAIL_BODY = "body";

	public static final String DETAIL_IDS = "ids";

	public static final String DETAIL_USER = "userId";

	public static final String MESSAGE_TITLE_REQUIRED = "Name the task.";

	public static final String MESSAGE_TITLE_TOO_LONG = "Use " + TITLE_MAX_LENGTH + " characters or fewer.";

	public static final String MESSAGE_DESCRIPTION_TOO_LONG = "Use " + DESCRIPTION_MAX_LENGTH
			+ " characters or fewer.";

	public static final String MESSAGE_PRIORITY_REQUIRED = "Choose a priority.";

	public static final String MESSAGE_DATES_INVALID = "The due date can't be before the start date.";

	public static final String MESSAGE_WORK_HOURS_INVALID = "Enter hours from 0 to " + WORK_HOURS_MAX
			+ ", with at most " + WORK_HOURS_SCALE + " decimals.";

	public static final String MESSAGE_COMPLETION_INVALID = "Enter a whole number from 0 to " + COMPLETION_MAX + ".";

	public static final String MESSAGE_TAGS_TOO_MANY = "Use at most " + TAG_LIMIT + " tags.";

	public static final String MESSAGE_TAG_INVALID = "A tag is 1 to " + TAG_MAX_LENGTH + " characters.";

	public static final String MESSAGE_OWNERS_TOO_MANY = "Use at most " + OWNER_LIMIT + " owners.";

	public static final String MESSAGE_PERSON_INVALID = "Only active people in this portal can be chosen.";

	public static final String MESSAGE_STATUS_REQUIRED = "Choose a status.";

	public static final String MESSAGE_STATUS_INVALID = "That is not a status of this task's layout.";

	public static final String MESSAGE_LIST_REQUIRED = "Choose a task list.";

	public static final String MESSAGE_PARENT_INVALID = "That task is not in this project.";

	public static final String MESSAGE_PARENT_CYCLE = "A task cannot be under itself or its own subtasks.";

	public static final String MESSAGE_PARENT_TOO_DEEP = "Subtasks can go at most " + MAX_DEPTH + " levels deep.";

	public static final String MESSAGE_SUBTASK_LIST = "A subtask stays in its parent's list.";

	public static final String MESSAGE_NAME_REQUIRED = "Name the task list.";

	public static final String MESSAGE_NAME_TOO_LONG = "Use " + LIST_NAME_MAX_LENGTH + " characters or fewer.";

	public static final String MESSAGE_NAME_TAKEN = "A task list with this name already exists in this project.";

	public static final String MESSAGE_VISIBILITY_REQUIRED = "Choose Internal or External.";

	public static final String MESSAGE_IDS_INVALID = "List each task list of the project once.";

	public static final String MESSAGE_COMMENT_REQUIRED = "Write a comment.";

	public static final String MESSAGE_COMMENT_TOO_LONG = "Use " + COMMENT_MAX_LENGTH + " characters or fewer.";

	/** Cached task reads: the response keys carry the caller, the definitions version and the project's task version. */
	public static final String CACHE_LIST_KEY = "work:resp:tasks:";

	public static final String CACHE_DETAIL_KEY = "work:resp:task:";

	/** A task never changes project, so which project a task belongs to can be remembered for a while. */
	public static final String CACHE_TASK_PROJECT_KEY = "work:taskproj:";

	public static final Duration CACHE_TASK_PROJECT_TTL = Duration.ofMinutes(10);

	public static final String CACHE_SEPARATOR = ":";

	private TaskConstants() {
	}

}
