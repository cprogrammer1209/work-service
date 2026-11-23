package com.projecty.work.modules.task.internal.helper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.exception.AppException;
import com.projecty.work.modules.access.api.MemberAccessLookUpService;
import com.projecty.work.modules.access.api.dto.MemberAccessResponseDTO;
import com.projecty.work.modules.project.api.ProjectLookUpService;
import com.projecty.work.modules.project.api.dto.ProjectSummaryDTO;
import com.projecty.work.modules.task.internal.constants.TaskConstants;
import com.projecty.work.modules.task.internal.entity.Task;
import com.projecty.work.modules.task.internal.entity.TaskList;
import com.projecty.work.modules.task.internal.entity.TaskListVisibility;
import com.projecty.work.modules.task.internal.repository.TaskListRepository;
import com.projecty.work.modules.task.internal.repository.TaskRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Who may see what: the caller, the project (through the project module's port) and the Client rule (a Client never
 * sees tasks of an INTERNAL list). Anything the caller may not see is "not found".
 */
@Component
public class TaskAccess {

	private final MemberAccessLookUpService accessLookUpService;

	private final ProjectLookUpService projectLookUpService;

	private final TaskRepository taskRepository;

	private final TaskListRepository listRepository;

	public TaskAccess(MemberAccessLookUpService accessLookUpService, ProjectLookUpService projectLookUpService,
			TaskRepository taskRepository, TaskListRepository listRepository) {
		this.accessLookUpService = accessLookUpService;
		this.projectLookUpService = projectLookUpService;
		this.taskRepository = taskRepository;
		this.listRepository = listRepository;
	}

	/** A task the caller may see, with its project and list. */
	public record Found(Task task, ProjectSummaryDTO project, TaskList list) {
	}

	public TaskCaller caller(UUID portalId, UUID userId) {
		MemberAccessResponseDTO access = this.accessLookUpService.findMemberAccess(portalId, userId)
			.filter(MemberAccessResponseDTO::active)
			.orElseThrow(() -> new AppException(HttpStatus.FORBIDDEN, AppConstants.ERROR_PORTAL_ACCESS_DENIED,
					AppConstants.MESSAGE_PORTAL_ACCESS_DENIED));
		return new TaskCaller(userId, TaskConstants.PROFILE_ADMIN.equals(access.profileKey()),
				TaskConstants.PROFILE_CLIENT.equals(access.profileKey()), access.permissions());
	}

	public void require(TaskCaller caller, String permission, String message) {
		if (!caller.can(permission)) {
			throw new AppException(HttpStatus.FORBIDDEN, TaskConstants.ERROR_FORBIDDEN, message);
		}
	}

	/** The project, if the caller may see it; another portal's project and a hidden one are alike "not found". */
	public ProjectSummaryDTO project(UUID portalId, UUID projectId, UUID userId) {
		return this.projectLookUpService.findVisibleProject(portalId, projectId, userId)
			.orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, TaskConstants.ERROR_PROJECT_NOT_FOUND,
					TaskConstants.MESSAGE_PROJECT_NOT_FOUND));
	}

	/** The project if the caller may see it, else empty (for callers that answer with their own "not found"). */
	public Optional<ProjectSummaryDTO> projectOrNull(UUID portalId, UUID projectId, UUID userId) {
		return this.projectLookUpService.findVisibleProject(portalId, projectId, userId);
	}

	/**
	 * The task, if it is in the portal, its project is visible and, for a Client, its list is External. A trashed
	 * task is found only with {@code allowTrashed}. With {@code lock} the row stays locked until the transaction ends.
	 */
	public Found find(UUID portalId, UUID taskId, TaskCaller caller, boolean lock, boolean allowTrashed) {
		Task task = (lock ? this.taskRepository.lockByIdAndPortalId(taskId, portalId)
				: this.taskRepository.findByIdAndPortalId(taskId, portalId))
			.filter((found) -> allowTrashed || !found.isTrashed())
			.orElseThrow(TaskAccess::taskNotFound);
		ProjectSummaryDTO project = this.projectLookUpService
			.findVisibleProject(portalId, task.getProjectId(), caller.userId())
			.orElseThrow(TaskAccess::taskNotFound);
		TaskList list = this.listRepository.findByIdAndPortalId(task.getTaskListId(), portalId)
			.orElseThrow(TaskAccess::taskNotFound);
		if (caller.client() && list.getVisibility() != TaskListVisibility.EXTERNAL) {
			throw taskNotFound();
		}
		return new Found(task, project, list);
	}

	/** Every id must be an active person of the portal; otherwise a field error under {@code detail}. */
	public void requireActivePeople(UUID portalId, List<UUID> userIds, String detail) {
		for (UUID userId : userIds) {
			boolean active = this.accessLookUpService.findMemberAccess(portalId, userId)
				.filter(MemberAccessResponseDTO::active)
				.isPresent();
			if (!active) {
				Map<String, Object> details = new HashMap<>();
				details.put(detail, TaskConstants.MESSAGE_PERSON_INVALID);
				throw new AppException(HttpStatus.BAD_REQUEST, AppConstants.ERROR_VALIDATION_FAILED,
						AppConstants.MESSAGE_VALIDATION_FAILED, details);
			}
		}
	}

	public static AppException taskNotFound() {
		return new AppException(HttpStatus.NOT_FOUND, TaskConstants.ERROR_TASK_NOT_FOUND,
				TaskConstants.MESSAGE_TASK_NOT_FOUND);
	}

}
