package com.projecty.work.modules.shared.helper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;

import com.projecty.work.modules.shared.constants.CacheKeyConstants;

/**
 * Builds the parts of a cache key that depend on who is asking. A response differs per user (what they may edit, which
 * projects they see), so the key carries the user and a fingerprint of their access; a profile or permission change
 * then gives a new key by itself and nothing has to be evicted.
 */
public final class CacheKeyHelper {

	/** Profile plus a hash of the sorted permissions, e.g. {@code MANAGER:3fa9c1d2e07b44aa}. */
	public static String fingerprint(String profileKey, Collection<String> permissions) {
		String sorted = String.join(CacheKeyConstants.PERMISSION_SEPARATOR, permissions.stream().sorted().toList());
		return (profileKey == null ? "" : profileKey) + CacheKeyConstants.SEPARATOR + hash(sorted);
	}

	/** The same for a caller already reduced to what the project and task rules look at. */
	public static String fingerprint(boolean admin, boolean client, Collection<String> permissions) {
		return fingerprint(admin ? CacheKeyConstants.PROFILE_ADMIN
				: client ? CacheKeyConstants.PROFILE_CLIENT : CacheKeyConstants.PROFILE_OTHER, permissions);
	}

	/** A short stable hash of any text, for keys that would otherwise carry a long value (a set of filters). */
	public static String hash(String text) {
		try {
			byte[] digest = MessageDigest.getInstance(CacheKeyConstants.HASH_ALGORITHM)
				.digest(text.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest).substring(0, CacheKeyConstants.HASH_LENGTH);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private CacheKeyHelper() {
	}

}
