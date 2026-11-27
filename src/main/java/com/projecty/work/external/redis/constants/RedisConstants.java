package com.projecty.work.external.redis.constants;

import java.time.Duration;

public final class RedisConstants {

	public static final String UNAVAILABLE = "Redis {} failed for key {}, carrying on without it for {}: {}";

	public static final String KEY_SEPARATOR = ":";

	/** A version lives at {@code <scope>:ver}; a missing version reads as {@code 0}. */
	public static final String VERSION_SUFFIX = ":ver";

	public static final String VERSION_UNSET = "0";

	/** Longer than any cached response lives, so a version that expired and restarted cannot meet an old entry. */
	public static final Duration VERSION_TTL = Duration.ofDays(1);

	public static final String CACHE_STORE_FAILED = "Could not cache {}: {}";

	private RedisConstants() {
	}

}
