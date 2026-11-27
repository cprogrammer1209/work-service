package com.projecty.work.external.redis.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.projecty.work.external.redis.config.RedisProperties;
import com.projecty.work.external.redis.constants.RedisConstants;
import com.projecty.work.external.redis.service.KeyValueStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "projecty.redis.type", havingValue = "redis", matchIfMissing = true)
public class RedisKeyValueStore implements KeyValueStore {

	private static final Logger log = LoggerFactory.getLogger(RedisKeyValueStore.class);

	private final StringRedisTemplate redis;

	private final RedisProperties properties;

	private final Clock clock;

	/** While Redis is down every call would wait for its own timeout, so after a failure it is left alone for a while. */
	private volatile Instant pausedUntil = Instant.MIN;

	public RedisKeyValueStore(StringRedisTemplate redis, RedisProperties properties, Clock clock) {
		this.redis = redis;
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	public Optional<String> get(String key) {
		return guard("GET", key, Optional.empty(), () -> Optional.ofNullable(this.redis.opsForValue().get(key)));
	}

	@Override
	public void put(String key, String value, Duration ttl) {
		guard("SET", key, null, () -> {
			this.redis.opsForValue().set(key, value, ttl);
			return null;
		});
	}

	@Override
	public void delete(String key) {
		guard("DEL", key, null, () -> this.redis.delete(key));
	}

	@Override
	public boolean exists(String key) {
		return guard("EXISTS", key, false, () -> Boolean.TRUE.equals(this.redis.hasKey(key)));
	}

	@Override
	public long increment(String key, Duration ttl) {
		return guard("INCR", key, 0L, () -> {
			// setIfAbsent sets the expiry together with the key, so a crash cannot leave a counter that never expires
			this.redis.opsForValue().setIfAbsent(key, "0", ttl);
			Long value = this.redis.opsForValue().increment(key);
			return value == null ? 0L : value;
		});
	}

	@Override
	public Optional<Duration> timeToLive(String key) {
		return guard("TTL", key, Optional.empty(), () -> {
			Long seconds = this.redis.getExpire(key, TimeUnit.SECONDS);
			return seconds == null || seconds < 0 ? Optional.<Duration>empty() : Optional.of(Duration.ofSeconds(seconds));
		});
	}

	private <T> T guard(String operation, String key, T fallback, Supplier<T> call) {
		if (this.clock.instant().isBefore(this.pausedUntil)) {
			return fallback;
		}
		try {
			return call.get();
		}
		catch (RuntimeException ex) {
			this.pausedUntil = this.clock.instant().plus(this.properties.backoff());
			log.warn(RedisConstants.UNAVAILABLE, operation, key, this.properties.backoff(), ex.getMessage());
			return fallback;
		}
	}

}
