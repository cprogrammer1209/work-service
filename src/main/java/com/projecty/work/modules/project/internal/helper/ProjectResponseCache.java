package com.projecty.work.modules.project.internal.helper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import com.projecty.work.external.redis.config.RedisProperties;
import com.projecty.work.external.redis.service.CacheService;
import com.projecty.work.modules.customfield.api.CustomFieldLookUpService;
import com.projecty.work.modules.project.api.ProjectCacheService;
import com.projecty.work.modules.project.api.dto.ProjectSummaryDTO;
import com.projecty.work.modules.project.internal.constants.ProjectConstants;
import com.projecty.work.modules.project.internal.dto.ProjectDetailResponseDTO;
import com.projecty.work.modules.project.internal.dto.ProjectResponseDTO;

import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/**
 * Hit-or-miss caching of the project reads. A response differs per user (what they may edit, which projects they
 * see), so every key carries the user and a fingerprint of their access; it also carries the versions of what the
 * response is built from (the portal's definitions, the project, or the project list). A change moves a version, so
 * the next read misses and loads fresh data. The versions are read before the database, never after.
 */
@Component
public class ProjectResponseCache {

	/** A cached "may this user see the project": the summary, or null for "no" (also cached). */
	public record Visible(ProjectSummaryDTO project) {
	}

	private final CacheService cacheService;

	private final RedisProperties properties;

	private final ProjectCacheService projectCache;

	private final CustomFieldLookUpService customFieldLookUpService;

	public ProjectResponseCache(CacheService cacheService, RedisProperties properties,
			ProjectCacheService projectCache, CustomFieldLookUpService customFieldLookUpService) {
		this.cacheService = cacheService;
		this.properties = properties;
		this.projectCache = projectCache;
		this.customFieldLookUpService = customFieldLookUpService;
	}

	public Optional<ProjectSummaryDTO> visible(UUID portalId, UUID projectId, UUID userId, String fingerprint,
			Supplier<Optional<ProjectSummaryDTO>> loader) {
		String key = ProjectConstants.CACHE_VISIBLE_KEY + join(portalId, projectId, userId, fingerprint,
				this.projectCache.projectVersion(portalId, projectId));
		Visible visible = this.cacheService.read(key, new TypeReference<Visible>() {
		}, this.properties.responseTtl(), () -> new Visible(loader.get().orElse(null)));
		return Optional.ofNullable(visible.project());
	}

	public ProjectDetailResponseDTO detail(UUID portalId, UUID projectId, UUID userId, String fingerprint,
			Supplier<ProjectDetailResponseDTO> loader) {
		String key = ProjectConstants.CACHE_DETAIL_KEY + join(portalId, projectId, userId, fingerprint,
				this.customFieldLookUpService.definitionsVersion(portalId),
				this.projectCache.projectVersion(portalId, projectId));
		return this.cacheService.read(key, new TypeReference<ProjectDetailResponseDTO>() {
		}, this.properties.responseTtl(), loader);
	}

	public List<ProjectResponseDTO> list(UUID portalId, UUID userId, String fingerprint,
			Supplier<List<ProjectResponseDTO>> loader) {
		String key = ProjectConstants.CACHE_LIST_KEY + join(portalId, userId, fingerprint,
				this.customFieldLookUpService.definitionsVersion(portalId), this.projectCache.listVersion(portalId));
		return this.cacheService.read(key, new TypeReference<List<ProjectResponseDTO>>() {
		}, this.properties.responseTtl(), loader);
	}

	private static String join(Object... parts) {
		StringBuilder key = new StringBuilder();
		for (Object part : parts) {
			if (key.length() > 0) {
				key.append(ProjectConstants.CACHE_SEPARATOR);
			}
			key.append(part);
		}
		return key.toString();
	}

}
