package com.projecty.work.common.constants;

/**
 * App-wide constants: error codes and security roles not owned by any module.
 */
public final class AppConstants {

	/**
	 * Role of a customer access token (audience {@code projecty-api}).
	 */
	public static final String ROLE_USER = "USER";


	public static final String ROLE_PREFIX = "ROLE_";

	/**
	 * The portal a customer request acts in (PTL-US-04). Must be one of the token's {@code portals}.
	 */
	public static final String HEADER_PORTAL_ID = "X-Portal-Id";

	/**
	 * Access token claim listing the ids of the portals the user may act in.
	 */
	public static final String CLAIM_PORTALS = "portals";

	/**
	 * Access token claim with the id of the session the token belongs to.
	 */
	public static final String CLAIM_SESSION_ID = "sid";

	/**
	 * Key of a denied session: {@code denylist:sid:{sessionId}}. Shared contract with platform-service, which writes
	 * it when a session ends (logout, password reset, support session ended).
	 */
	public static final String DENYLIST_KEY_PREFIX = "denylist:sid:";

	public static final String ERROR_SESSION_ENDED = "invalid_token";

	public static final String MESSAGE_SESSION_ENDED = "The session of this token has ended.";

	/**
	 * Access token claim with the user's full name.
	 */
	public static final String CLAIM_NAME = "name";

	/**
	 * Request attribute holding the verified portal id (set by PortalContextFilter).
	 */
	public static final String REQUEST_ATTRIBUTE_PORTAL_ID = "projecty.portalId";

	public static final String MESSAGE_PORTAL_ID_INVALID = "X-Portal-Id must be a portal id.";

	public static final String ERROR_PORTAL_REQUIRED = "PORTAL_REQUIRED";

	public static final String ERROR_PORTAL_ACCESS_DENIED = "PORTAL_ACCESS_DENIED";

	public static final String MESSAGE_PORTAL_REQUIRED = "Choose a portal for this request (X-Portal-Id header).";

	public static final String MESSAGE_PORTAL_ACCESS_DENIED = "You don't have access to this portal.";

	public static final String ERROR_VALIDATION_FAILED = "VALIDATION_FAILED";

	public static final String ERROR_UNAUTHORIZED = "UNAUTHORIZED";

	public static final String ERROR_FORBIDDEN = "FORBIDDEN";

	public static final String ERROR_INTERNAL = "INTERNAL_ERROR";

	public static final String MESSAGE_VALIDATION_FAILED = "Some fields are invalid.";

	public static final String MESSAGE_UNAUTHORIZED = "Authentication is required.";

	public static final String MESSAGE_FORBIDDEN = "You don't have permission to do this.";

	public static final String MESSAGE_INTERNAL = "Something went wrong. Please try again.";

	private AppConstants() {
	}

}
