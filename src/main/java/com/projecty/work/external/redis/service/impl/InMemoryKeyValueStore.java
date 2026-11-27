package com.projecty.work.external.redis.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.projecty.work.external.redis.service.KeyValueStore;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Same behaviour inside this process, so tests and a run without Redis need no server. */
@Service
@ConditionalOnProperty(name = "projecty.redis.type", havingValue = "memory")
public class InMemoryKeyValueStore implements KeyValueStore {

	private record Entry(String value, Instant expiresAt) {
	}

	private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

	private final Clock clock;

	public InMemoryKeyValueStore(Clock clock) {
		this.clock = clock;
	}

	@Override
	public Optional<String> get(String key) {
		return live(key).map(Entry::value);
	}

	@Override
	public void put(String key, String value, Duration ttl) {
		this.entries.put(key, new Entry(value, this.clock.instant().plus(ttl)));
	}

	@Override
	public void delete(String key) {
		this.entries.remove(key);
	}

	@Override
	public boolean exists(String key) {
		return live(key).isPresent();
	}

	@Override
	public long increment(String key, Duration ttl) {
		Instant now = this.clock.instant();
		Entry updated = this.entries.compute(key, (k, old) -> old == null || !old.expiresAt().isAfter(now)
				? new Entry("1", now.plus(ttl)) : new Entry(String.valueOf(Long.parseLong(old.value()) + 1), old.expiresAt()));
		return Long.parseLong(updated.value());
	}

	@Override
	public Optional<Duration> timeToLive(String key) {
		return live(key).map((entry) -> Duration.between(this.clock.instant(), entry.expiresAt()));
	}

	private Optional<Entry> live(String key) {
		Entry entry = this.entries.get(key);
		if (entry == null) {
			return Optional.empty();
		}
		if (!entry.expiresAt().isAfter(this.clock.instant())) {
			this.entries.remove(key, entry);
			return Optional.empty();
		}
		return Optional.of(entry);
	}

}
