package com.projecty.work.modules.access.internal.helper;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.projecty.work.external.redis.config.RedisProperties;
import com.projecty.work.external.redis.service.KeyValueStore;
import com.projecty.work.modules.access.internal.constants.AccessConstants;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Remembers a user's access row in a portal (is there one, is it active, which profile, which permissions), so the
 * per-request checks and the services' own {@code findMemberAccess} do not
 * hit the database. Applying a member event evicts the entry after its transaction commits (a rolled-back event
 * must not evict, and a redelivery changes nothing); the TTL covers the rare read that races an eviction.
 */
@Component
public class MemberAccessCache {

	/** What is known about a user in a portal: no row at all, an inactive row, or an active one. */
	public record Access(boolean exists, boolean active, String profileKey, Set<String> permissions) {

		public static Access none() {
			return new Access(false, false, null, Set.of());
		}

	}

	private final KeyValueStore store;

	private final RedisProperties properties;

	public MemberAccessCache(KeyValueStore store, RedisProperties properties) {
		this.store = store;
		this.properties = properties;
	}

	public Optional<Access> find(UUID portalId, UUID userId) {
		return this.store.get(key(portalId, userId)).map(MemberAccessCache::parse);
	}

	public void put(UUID portalId, UUID userId, Access access) {
		this.store.put(key(portalId, userId), format(access), this.properties.accessTtl());
	}

	/** Evicts once the surrounding transaction has committed (at once when there is none). */
	public void evictAfterCommit(UUID portalId, UUID userId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			this.store.delete(key(portalId, userId));
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				MemberAccessCache.this.store.delete(key(portalId, userId));
			}
		});
	}

	private static String key(UUID portalId, UUID userId) {
		return AccessConstants.ACCESS_KEY_PREFIX + portalId + AccessConstants.ACCESS_SEPARATOR + userId;
	}

	/** {@code N:} for no row, otherwise {@code A:PROFILE:PERM,PERM} or {@code I:PROFILE:PERM,PERM}. */
	private static String format(Access access) {
		if (!access.exists()) {
			return AccessConstants.ACCESS_NONE + AccessConstants.ACCESS_SEPARATOR;
		}
		return (access.active() ? AccessConstants.ACCESS_ACTIVE : AccessConstants.ACCESS_INACTIVE)
				+ AccessConstants.ACCESS_SEPARATOR + (access.profileKey() == null ? "" : access.profileKey())
				+ AccessConstants.ACCESS_SEPARATOR
				+ access.permissions().stream().sorted().collect(Collectors.joining(AccessConstants.PERMISSION_SEPARATOR));
	}

	private static Access parse(String value) {
		if (value.startsWith(AccessConstants.ACCESS_NONE)) {
			return Access.none();
		}
		String[] parts = value.split(AccessConstants.ACCESS_SEPARATOR, 3);
		boolean active = parts[0].equals(AccessConstants.ACCESS_ACTIVE);
		String profileKey = parts.length > 1 && !parts[1].isEmpty() ? parts[1] : null;
		String rest = parts.length > 2 ? parts[2] : "";
		Set<String> permissions = rest.isEmpty() ? Set.of()
				: Arrays.stream(rest.split(AccessConstants.PERMISSION_SEPARATOR)).collect(Collectors.toUnmodifiableSet());
		return new Access(true, active, profileKey, permissions);
	}

}
