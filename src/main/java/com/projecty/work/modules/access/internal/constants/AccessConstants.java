package com.projecty.work.modules.access.internal.constants;

/**
 * Constants of the access module: the Platform topic and events it reads, and their payload fields.
 */
public final class AccessConstants {

	/**
	 * Platform's member topic. Events are keyed by user id, so one person's events arrive in order.
	 */
	public static final String TOPIC_MEMBER = "projecty.member";

	/**
	 * Work's own consumer group, so it reads every member event regardless of other services.
	 */
	public static final String GROUP_ID = "work-service";

	public static final String EVENT_USER_ACTIVATED = "UserActivated";

	public static final String EVENT_USER_PROFILE_CHANGED = "UserProfileChanged";

	public static final String EVENT_USER_DEACTIVATED = "UserDeactivated";

	public static final String FIELD_EVENT_ID = "eventId";

	public static final String FIELD_TYPE = "type";

	public static final String FIELD_PAYLOAD = "payload";

	public static final String FIELD_PORTAL_ID = "portalId";

	public static final String FIELD_USER_ID = "userId";

	public static final String FIELD_PROFILE_KEY = "profileKey";

	public static final String FIELD_PERMISSIONS = "permissions";

	private AccessConstants() {
	}

}
