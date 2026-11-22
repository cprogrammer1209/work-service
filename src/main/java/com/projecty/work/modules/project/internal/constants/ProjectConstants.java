package com.projecty.work.modules.project.internal.constants;

import java.util.List;

/**
 * Constants of the project module: key and field rules, the default statuses, error codes and activity names.
 */
public final class ProjectConstants {

	/**
	 * PRJ-BR-01: 2 to 10 capital letters or digits.
	 */
	public static final String KEY_PATTERN = "^[A-Z0-9]{2,10}$";

	public static final int NAME_MAX_LENGTH = 100;

	public static final int DESCRIPTION_MAX_LENGTH = 5000;

	public static final int DETAIL_MAX_LENGTH = 500;

	/**
	 * How many recent activity entries the project overview shows.
	 */
	public static final int ACTIVITY_LIMIT = 6;

	/**
	 * Platform profile keys the project rules care about (Admin bypasses project roles; Clients never see public
	 * projects).
	 */
	public static final String PROFILE_ADMIN = "ADMIN";

	public static final String PROFILE_CLIENT = "CLIENT";

	public static final String STATUS_DEFAULT = "Active";

	public static final String STATUS_TYPE_ACTIVE = "Active";

	public static final String STATUS_TYPE_ON_HOLD = "On hold";

	public static final String STATUS_TYPE_COMPLETED = "Completed";

	public static final String STATUS_TYPE_CANCELLED = "Cancelled";

	/**
	 * The portal's project statuses. Customizing them is a V1 story (PRJ-US-11), so the default list is fixed.
	 */
	public static final List<StatusDefinition> STATUSES = List.of(new StatusDefinition("Active", STATUS_TYPE_ACTIVE),
			new StatusDefinition("In Progress", STATUS_TYPE_ACTIVE), new StatusDefinition("On Track", STATUS_TYPE_ACTIVE),
			new StatusDefinition("Delayed", STATUS_TYPE_ACTIVE), new StatusDefinition("On Hold", STATUS_TYPE_ON_HOLD),
			new StatusDefinition("Completed", STATUS_TYPE_COMPLETED),
			new StatusDefinition("Cancelled", STATUS_TYPE_CANCELLED));

	public static final String ACTION_CREATED = "PROJECT_CREATED";

	public static final String ACTION_UPDATED = "PROJECT_UPDATED";

	public static final String ACTION_STATUS_CHANGED = "PROJECT_STATUS_CHANGED";

	public static final String ACTION_TRASHED = "PROJECT_TRASHED";

	public static final String ACTION_RESTORED = "PROJECT_RESTORED";

	public static final String ACTION_MEMBER_ADDED = "PROJECT_MEMBER_ADDED";

	public static final String ACTION_MEMBER_REMOVED = "PROJECT_MEMBER_REMOVED";

	public static final String ERROR_PROJECT_NOT_FOUND = "PROJECT_NOT_FOUND";

	public static final String ERROR_PROJECT_FORBIDDEN = "PROJECT_FORBIDDEN";

	public static final String ERROR_KEY_TAKEN = "PROJECT_KEY_TAKEN";

	public static final String ERROR_DATES_INVALID = "PROJECT_DATES_INVALID";

	public static final String ERROR_STATUS_INVALID = "PROJECT_STATUS_INVALID";

	public static final String ERROR_PERSON_INVALID = "PROJECT_PERSON_INVALID";

	public static final String ERROR_ALREADY_MEMBER = "PROJECT_ALREADY_MEMBER";

	public static final String ERROR_MEMBER_NOT_FOUND = "PROJECT_MEMBER_NOT_FOUND";

	public static final String ERROR_OWNER_LOCKED = "PROJECT_OWNER_LOCKED";

	public static final String MESSAGE_PROJECT_NOT_FOUND = "That project does not exist, or you cannot see it.";

	public static final String MESSAGE_FORBIDDEN_EDIT = "Only Admins, the project owner and project Managers can change this project.";

	public static final String MESSAGE_FORBIDDEN_DELETE = "Only the project owner or an Admin can delete or restore a project.";

	public static final String MESSAGE_KEY_TAKEN = "That key is already used in this portal.";

	public static final String MESSAGE_DATES_INVALID = "The end date can't be before the start date.";

	public static final String MESSAGE_STATUS_INVALID = "That is not a project status.";

	public static final String MESSAGE_PERSON_INVALID = "Only active people in this portal can own or join a project.";

	public static final String MESSAGE_ALREADY_MEMBER = "That person is already in this project.";

	public static final String MESSAGE_MEMBER_NOT_FOUND = "That person is not in this project.";

	public static final String MESSAGE_OWNER_LOCKED = "The owner always stays a member. Change the owner first.";

	/**
	 * @param type Active, On hold, Completed or Cancelled
	 */
	public record StatusDefinition(String name, String type) {
	}

	private ProjectConstants() {
	}

}
