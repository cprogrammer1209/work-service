package com.projecty.work.modules.customfield.internal.helper;

import java.util.UUID;
import java.util.function.Supplier;

import com.projecty.work.external.redis.config.RedisProperties;
import com.projecty.work.external.redis.service.KeyValueStore;
import com.projecty.work.modules.customfield.internal.constants.CustomFieldConstants;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Remembers field, layout and status definitions, which are read on every project and task screen and change only
 * when an admin edits them. Entries are filed under the portal's current version; a change bumps the version after
 * commit, so nothing has to be searched for and deleted. The version is read before the database, which keeps a
 * read that races a change from ever being filed under the new version.
 */
@Component
public class DefinitionCache {

	private final KeyValueStore store;

	private final RedisProperties properties;

	private final ObjectMapper objectMapper;

	public DefinitionCache(KeyValueStore store, RedisProperties properties, ObjectMapper objectMapper) {
		this.store = store;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	/**
	 * Returns the cached value, or loads and caches it. Only a read-only transaction uses the cache: a transaction
	 * that is changing data must see its own writes, and the version only moves after it commits.
	 */
	public <T> T read(UUID portalId, String what, TypeReference<T> type, Supplier<T> loader) {
		if (!TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
			return loader.get();
		}
		String version = version(portalId);
		String key = CustomFieldConstants.DEFINITIONS_KEY_PREFIX + portalId + CustomFieldConstants.DEFINITIONS_SEPARATOR
				+ version + CustomFieldConstants.DEFINITIONS_SEPARATOR + what;
		T cached = this.store.get(key).map((json) -> parse(json, type)).orElse(null);
		if (cached != null) {
			return cached;
		}
		T loaded = loader.get();
		this.store.put(key, this.objectMapper.writeValueAsString(loaded), this.properties.definitionsTtl());
		return loaded;
	}

	/** Makes every cached definition of the portal unreachable once the surrounding transaction has committed. */
	public void bumpAfterCommit(UUID portalId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			bump(portalId);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				bump(portalId);
			}
		});
	}

	/** The portal's current definitions version; cached responses that embed definitions carry it in their key. */
	public String version(UUID portalId) {
		return this.store.get(versionKey(portalId)).orElse(CustomFieldConstants.DEFINITIONS_UNVERSIONED);
	}

	private void bump(UUID portalId) {
		this.store.increment(versionKey(portalId), CustomFieldConstants.DEFINITIONS_VERSION_TTL);
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

	private static String versionKey(UUID portalId) {
		return CustomFieldConstants.DEFINITIONS_KEY_PREFIX + portalId + CustomFieldConstants.DEFINITIONS_SEPARATOR
				+ CustomFieldConstants.DEFINITIONS_VERSION_SUFFIX;
	}

}
