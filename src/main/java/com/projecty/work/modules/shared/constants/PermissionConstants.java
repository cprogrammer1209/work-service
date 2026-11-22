package com.projecty.work.modules.shared.constants;

/**
 * Permission names Work checks with {@code @RequirePermission}. Platform owns them: it grants them to profiles
 * (MemberConstants.SYSTEM_PROFILES there) and announces each member's permissions in member events. A name here
 * must exist there.
 */
public final class PermissionConstants {

	public static final String PROJECTS_VIEW = "PROJECTS_VIEW";

	public static final String PROJECTS_CREATE = "PROJECTS_CREATE";

	public static final String PROJECTS_EDIT = "PROJECTS_EDIT";

	public static final String PROJECTS_DELETE = "PROJECTS_DELETE";

	public static final String CUSTOM_FIELDS_VIEW = "CUSTOM_FIELDS_VIEW";

	public static final String CUSTOM_FIELDS_MANAGE = "CUSTOM_FIELDS_MANAGE";

	private PermissionConstants() {
	}

}
