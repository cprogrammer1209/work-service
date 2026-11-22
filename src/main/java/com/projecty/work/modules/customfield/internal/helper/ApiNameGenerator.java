package com.projecty.work.modules.customfield.internal.helper;

import java.util.Locale;
import java.util.UUID;

import com.projecty.work.modules.customfield.internal.constants.CustomFieldConstants;
import com.projecty.work.modules.customfield.internal.repository.CustomFieldRepository;

import org.springframework.stereotype.Component;

/**
 * Makes the key of a field from its label: {@code cf_} plus snake_case, with a number added when taken (CUS-BR-01).
 * Deleted fields count as taken, so a key is never reused.
 */
@Component
public class ApiNameGenerator {

	private final CustomFieldRepository fieldRepository;

	public ApiNameGenerator(CustomFieldRepository fieldRepository) {
		this.fieldRepository = fieldRepository;
	}

	public String generate(UUID portalId, String entityType, String label) {
		String slug = label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
		if (slug.isEmpty()) {
			slug = CustomFieldConstants.KEY_FALLBACK;
		}
		// leave room for a "_NN" suffix inside the length limit
		int room = CustomFieldConstants.API_NAME_MAX_LENGTH - CustomFieldConstants.KEY_PREFIX.length() - 4;
		String base = CustomFieldConstants.KEY_PREFIX + (slug.length() > room ? slug.substring(0, room) : slug);
		String candidate = base;
		for (int n = 2; this.fieldRepository.existsByPortalIdAndEntityTypeAndApiName(portalId, entityType,
				candidate); n++) {
			candidate = base + "_" + n;
		}
		return candidate;
	}

}
