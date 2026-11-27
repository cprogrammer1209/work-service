package com.projecty.work.external.redis.service;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import tools.jackson.core.type.TypeReference;

/**
 * Hit-or-miss caching of a read, on top of {@link KeyValueStore}: look in Redis, and on a miss load the value, store
 * it and return it. Values are kept as JSON. Like the store it never throws for a Redis problem: an unavailable or
 * unreadable entry is simply a miss.
 *
 * Changes are handled with versions, not by searching for keys: a key contains the current version of the data it
 * depends on ({@link #version}), and a write moves that version after its transaction commits
 * ({@link #bumpAfterCommit}), so older entries are never read again and expire by their TTL.
 */
public interface CacheService {

	/**
	 * Returns the cached value, or runs {@code loader}, caches its result for {@code ttl} and returns it. Only a
	 * read-only transaction uses the cache: a transaction that changes data must see its own writes, and versions only
	 * move after it commits. A null result is returned but not cached.
	 */
	<T> T read(String key, TypeReference<T> type, Duration ttl, Supplier<T> loader);

	/**
	 * Reads one entry without loading it, for lookups that fetch many ids and want a single database call for the
	 * ones that are missing. Empty on a miss, and always empty outside a read-only transaction.
	 */
	<T> Optional<T> peek(String key, TypeReference<T> type);

	/** Stores one entry for {@code ttl}. Does nothing outside a read-only transaction, like {@link #read}. */
	void put(String key, Object value, Duration ttl);

	/** The current version of a scope, {@code "0"} when it never changed. Read it before loading the data. */
	String version(String scope);

	/** Moves the scope's version once the surrounding transaction has committed (at once when there is none). */
	void bumpAfterCommit(String scope);

	/** Deletes the keys once the surrounding transaction has committed (at once when there is none). */
	void evictAfterCommit(String... keys);

}
