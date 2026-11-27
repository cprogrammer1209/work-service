package com.projecty.work.external.redis.service.impl;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import com.projecty.work.external.redis.constants.RedisConstants;
import com.projecty.work.external.redis.service.CacheService;
import com.projecty.work.external.redis.service.KeyValueStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class RedisCacheService implements CacheService {

	private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

	private final KeyValueStore store;

	private final ObjectMapper objectMapper;

	public RedisCacheService(KeyValueStore store, ObjectMapper objectMapper) {
		this.store = store;
		this.objectMapper = objectMapper;
	}

	@Override
	public <T> T read(String key, TypeReference<T> type, Duration ttl, Supplier<T> loader) {
		if (!TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
			return loader.get();
		}
		T cached = this.store.get(key).map((json) -> parse(json, type)).orElse(null);
		if (cached != null) {
			return cached;
		}
		T loaded = loader.get();
		store(key, loaded, ttl);
		return loaded;
	}

	@Override
	public <T> Optional<T> peek(String key, TypeReference<T> type) {
		if (!TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
			return Optional.empty();
		}
		return this.store.get(key).map((json) -> parse(json, type));
	}

	@Override
	public void put(String key, Object value, Duration ttl) {
		if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
			store(key, value, ttl);
		}
	}

	@Override
	public String version(String scope) {
		return this.store.get(scope + RedisConstants.VERSION_SUFFIX).orElse(RedisConstants.VERSION_UNSET);
	}

	@Override
	public void bumpAfterCommit(String scope) {
		afterCommit(() -> this.store.increment(scope + RedisConstants.VERSION_SUFFIX, RedisConstants.VERSION_TTL));
	}

	@Override
	public void evictAfterCommit(String... keys) {
		afterCommit(() -> {
			for (String key : keys) {
				this.store.delete(key);
			}
		});
	}

	private void store(String key, Object value, Duration ttl) {
		if (value == null) {
			return;
		}
		try {
			this.store.put(key, this.objectMapper.writeValueAsString(value), ttl);
		}
		catch (RuntimeException ex) {
			// A value that cannot be written as JSON is served from the database every time, never an error.
			log.warn(RedisConstants.CACHE_STORE_FAILED, key, ex.getMessage());
		}
	}

	private <T> T parse(String json, TypeReference<T> type) {
		try {
			return this.objectMapper.readValue(json, type);
		}
		catch (RuntimeException ex) {
			// An entry that cannot be read (for example after a DTO changed) counts as missing and is replaced.
			return null;
		}
	}

	private static void afterCommit(Runnable action) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			action.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				action.run();
			}
		});
	}

}
