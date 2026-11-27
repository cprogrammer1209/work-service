package com.projecty.work.modules.task.internal.helper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import com.projecty.work.external.redis.config.RedisProperties;
import com.projecty.work.external.redis.service.CacheService;
import com.projecty.work.modules.customfield.api.CustomFieldLookUpService;
import com.projecty.work.modules.project.api.ProjectCacheService;
import com.projecty.work.modules.task.internal.constants.TaskConstants;
import com.projecty.work.modules.task.internal.dto.TaskDetailResponseDTO;
import com.projecty.work.modules.task.internal.dto.TaskResponseDTO;

import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/**
 * Hit-or-miss caching of the task reads. A response differs per user (what they may edit, which lists a Client may
 * see), so every key carries the user and a fingerprint of their access. It also carries the portal's definitions
 * version and the project's task version, which moves on every task, comment or task list change and whenever the
 * project itself changes. The versions are read before the database, never after.
 */
@Component
public class TaskResponseCache {

	private final CacheService cacheService;

	private final RedisProperties properties;

	private final ProjectCacheService projectCache;

	private final CustomFieldLookUpService customFieldLookUpService;

	public TaskResponseCache(CacheService cacheService, RedisProperties properties, ProjectCacheService projectCache,
			CustomFieldLookUpService customFieldLookUpService) {
		this.cacheService = cacheService;
		this.properties = properties;
		this.projectCache = projectCache;
		this.customFieldLookUpService = customFieldLookUpService;
	}

	public List<TaskResponseDTO> list(UUID portalId, UUID projectId, UUID userId, String fingerprint,
			String filterHash, Supplier<List<TaskResponseDTO>> loader) {
		String key = TaskConstants.CACHE_LIST_KEY + join(portalId, projectId, userId, fingerprint,
				this.customFieldLookUpService.definitionsVersion(portalId),
				this.projectCache.taskVersion(portalId, projectId), filterHash);
		return this.cacheService.read(key, new TypeReference<List<TaskResponseDTO>>() {
		}, this.properties.responseTtl(), loader);
	}

	/** The project a task belongs to, which its detail key needs for the version; empty if there is no such task. */
	public Optional<UUID> projectOf(UUID portalId, UUID taskId, Supplier<UUID> loader) {
		String key = TaskConstants.CACHE_TASK_PROJECT_KEY + join(portalId, taskId);
		return Optional.ofNullable(this.cacheService.read(key, new TypeReference<UUID>() {
		}, TaskConstants.CACHE_TASK_PROJECT_TTL, loader));
	}

	public TaskDetailResponseDTO detail(UUID portalId, UUID taskId, UUID projectId, UUID userId, String fingerprint,
			Supplier<TaskDetailResponseDTO> loader) {
		String key = TaskConstants.CACHE_DETAIL_KEY + join(portalId, taskId, userId, fingerprint,
				this.customFieldLookUpService.definitionsVersion(portalId),
				this.projectCache.taskVersion(portalId, projectId));
		return this.cacheService.read(key, new TypeReference<TaskDetailResponseDTO>() {
		}, this.properties.responseTtl(), loader);
	}

	private static String join(Object... parts) {
		StringBuilder key = new StringBuilder();
		for (Object part : parts) {
			if (key.length() > 0) {
				key.append(TaskConstants.CACHE_SEPARATOR);
			}
			key.append(part);
		}
		return key.toString();
	}

}
