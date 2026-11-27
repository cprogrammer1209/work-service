package com.projecty.work.external.redis.service;

import java.time.Duration;
import java.util.Optional;

/**
 * Small key-value port over Redis for caches, counters and the token denylist. It never throws for a Redis
 * outage: a missing server looks like an empty store (no cached value, nothing denied, counter at 0), so callers
 * fall back to the database.
 */
public interface KeyValueStore {

	Optional<String> get(String key);

	void put(String key, String value, Duration ttl);

	void delete(String key);

	boolean exists(String key);

	/**
	 * Adds 1 to the counter and returns the new value. The counter lives for {@code ttl} from its first increment.
	 * Returns 0 when the store is unavailable.
	 */
	long increment(String key, Duration ttl);

	/** Time left before the key expires, empty when it does not exist or never expires. */
	Optional<Duration> timeToLive(String key);

}
