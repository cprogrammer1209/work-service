package com.projecty.work.modules.project.internal.service.impl;

import java.util.UUID;

import com.projecty.work.external.redis.service.CacheService;
import com.projecty.work.modules.project.api.ProjectCacheService;
import com.projecty.work.modules.project.internal.constants.ProjectConstants;

import org.springframework.stereotype.Service;

@Service
public class ProjectCacheServiceImpl implements ProjectCacheService {

	private final CacheService cacheService;

	public ProjectCacheServiceImpl(CacheService cacheService) {
		this.cacheService = cacheService;
	}

	@Override
	public String projectVersion(UUID portalId, UUID projectId) {
		return this.cacheService.version(projectScope(portalId, projectId));
	}

	@Override
	public String taskVersion(UUID portalId, UUID projectId) {
		return this.cacheService.version(taskScope(portalId, projectId));
	}

	@Override
	public String listVersion(UUID portalId) {
		return this.cacheService.version(listScope(portalId));
	}

	@Override
	public void projectChanged(UUID portalId, UUID projectId) {
		this.cacheService.bumpAfterCommit(projectScope(portalId, projectId));
		this.cacheService.bumpAfterCommit(taskScope(portalId, projectId));
		this.cacheService.bumpAfterCommit(listScope(portalId));
	}

	@Override
	public void tasksChanged(UUID portalId, UUID projectId) {
		this.cacheService.bumpAfterCommit(taskScope(portalId, projectId));
	}

	private static String projectScope(UUID portalId, UUID projectId) {
		return ProjectConstants.CACHE_PROJECT_SCOPE + portalId + ProjectConstants.CACHE_SEPARATOR + projectId;
	}

	private static String taskScope(UUID portalId, UUID projectId) {
		return ProjectConstants.CACHE_TASKS_SCOPE + portalId + ProjectConstants.CACHE_SEPARATOR + projectId;
	}

	private static String listScope(UUID portalId) {
		return ProjectConstants.CACHE_LIST_SCOPE + portalId;
	}

}
