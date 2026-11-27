package com.projecty.work.modules.shared.constants;

/** Pieces every cached read builds its key from. */
public final class CacheKeyConstants {

	public static final String SEPARATOR = ":";

	public static final String PERMISSION_SEPARATOR = ",";

	/** How a caller counts in a key: Admins and Clients are treated differently by project and task rules. */
	public static final String PROFILE_ADMIN = "ADMIN";

	public static final String PROFILE_CLIENT = "CLIENT";

	public static final String PROFILE_OTHER = "OTHER";

	public static final String HASH_ALGORITHM = "SHA-256";

	/** Hex characters of a hash kept in a key: enough to tell two filters apart, short enough to read. */
	public static final int HASH_LENGTH = 16;

	private CacheKeyConstants() {
	}

}
