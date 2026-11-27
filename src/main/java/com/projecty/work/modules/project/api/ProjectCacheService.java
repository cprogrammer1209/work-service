package com.projecty.work.modules.project.api;

import java.util.UUID;

/**
 * The versions that cached reads about projects and their tasks put in their keys, and the calls that move them.
 * A cached response is read under the current version, so moving a version drops every response that depended on it
 * without searching for keys; the old entries just expire.
 *
 * <ul>
 * <li>project version: the project row, its members and everything that decides who may see it (project detail,
 * visibility checks)</li>
 * <li>task version: the tasks, comments and task lists of a project, and also everything above, because task views
 * embed the project's key, name and visibility</li>
 * <li>list version: the portal's list of projects</li>
 * </ul>
 */
public interface ProjectCacheService {

	String projectVersion(UUID portalId, UUID projectId);

	String taskVersion(UUID portalId, UUID projectId);

	String listVersion(UUID portalId);

	/** A project, its members, status, layout or trash state changed (also moves the task and list versions). */
	void projectChanged(UUID portalId, UUID projectId);

	/** A task, comment or task list of the project changed. */
	void tasksChanged(UUID portalId, UUID projectId);

}
