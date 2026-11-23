package com.projecty.work.modules.task.internal.service.impl;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.exception.AppException;
import com.projecty.work.common.service.RequestPortalContext;
import com.projecty.work.modules.shared.constants.OutboxEventConstants;
import com.projecty.work.modules.task.internal.constants.TaskConstants;
import com.projecty.work.modules.task.internal.dto.TaskCommentBodyDTO;
import com.projecty.work.modules.task.internal.dto.TaskCommentResponseDTO;
import com.projecty.work.modules.task.internal.entity.TaskComment;
import com.projecty.work.modules.task.internal.helper.TaskAccess;
import com.projecty.work.modules.task.internal.helper.TaskCaller;
import com.projecty.work.modules.task.internal.helper.TaskHistory;
import com.projecty.work.modules.task.internal.mapper.TaskMapper;
import com.projecty.work.modules.task.internal.repository.TaskCommentRepository;
import com.projecty.work.modules.task.internal.repository.TaskRepository;
import com.projecty.work.modules.task.internal.service.TaskCommentService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskCommentServiceImpl implements TaskCommentService {

	private final TaskCommentRepository commentRepository;

	private final TaskRepository taskRepository;

	private final TaskAccess access;

	private final TaskHistory history;

	private final RequestPortalContext portalContext;

	private final TaskMapper mapper;

	private final Clock clock;

	public TaskCommentServiceImpl(TaskCommentRepository commentRepository, TaskRepository taskRepository,
			TaskAccess access, TaskHistory history, RequestPortalContext portalContext, TaskMapper mapper,
			Clock clock) {
		this.commentRepository = commentRepository;
		this.taskRepository = taskRepository;
		this.access = access;
		this.history = history;
		this.portalContext = portalContext;
		this.mapper = mapper;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public List<TaskCommentResponseDTO> list(UUID actorId, UUID taskId) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		this.access.find(portalId, taskId, caller, false, false);
		return this.commentRepository.findByPortalIdAndTaskIdOrderByCreatedAtAscIdAsc(portalId, taskId)
			.stream()
			.map((comment) -> view(comment, caller))
			.toList();
	}

	@Override
	@Transactional
	public TaskCommentResponseDTO add(UUID actorId, String actorName, UUID taskId, TaskCommentBodyDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		TaskAccess.Found found = this.access.find(portalId, taskId, caller, false, false);
		String body = requireBody(request.body());
		TaskComment comment = this.commentRepository
			.save(new TaskComment(portalId, taskId, actorId, actorName, body, this.clock.instant()));
		this.history.record(found.task(), TaskConstants.ACTION_COMMENT_ADDED, null, null, null, body, actorId, actorName);
		this.history.publish(OutboxEventConstants.EVENT_TASK_COMMENT_ADDED, found.task(), found.project().key(),
				actorId, actorName, Map.of("commentId", comment.getId()));
		return view(comment, caller);
	}

	@Override
	@Transactional
	public TaskCommentResponseDTO edit(UUID actorId, String actorName, UUID commentId, TaskCommentBodyDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		TaskComment comment = findComment(portalId, commentId);
		TaskAccess.Found found = this.access.find(portalId, comment.getTaskId(), caller, false, false);
		if (!comment.getAuthorUserId().equals(actorId)) {
			throw forbidden();
		}
		comment.edit(requireBody(request.body()), this.clock.instant());
		this.history.record(found.task(), TaskConstants.ACTION_COMMENT_EDITED, null, null, null, comment.getBody(),
				actorId, actorName);
		this.history.publish(OutboxEventConstants.EVENT_TASK_COMMENT_UPDATED, found.task(), found.project().key(),
				actorId, actorName, Map.of("commentId", comment.getId()));
		return view(comment, caller);
	}

	@Override
	@Transactional
	public void delete(UUID actorId, String actorName, UUID commentId) {
		UUID portalId = this.portalContext.requirePortalId();
		TaskCaller caller = this.access.caller(portalId, actorId);
		TaskComment comment = findComment(portalId, commentId);
		TaskAccess.Found found = this.access.find(portalId, comment.getTaskId(), caller, false, false);
		if (!comment.getAuthorUserId().equals(actorId) && !caller.admin()) {
			throw forbidden();
		}
		this.commentRepository.delete(comment);
		this.history.record(found.task(), TaskConstants.ACTION_COMMENT_DELETED, null, null, null, null, actorId,
				actorName);
		this.history.publish(OutboxEventConstants.EVENT_TASK_COMMENT_DELETED, found.task(), found.project().key(),
				actorId, actorName, Map.of("commentId", commentId));
	}

	private TaskComment findComment(UUID portalId, UUID commentId) {
		return this.commentRepository.findByIdAndPortalId(commentId, portalId)
			.orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, TaskConstants.ERROR_COMMENT_NOT_FOUND,
					TaskConstants.MESSAGE_COMMENT_NOT_FOUND));
	}

	private TaskCommentResponseDTO view(TaskComment comment, TaskCaller caller) {
		boolean own = comment.getAuthorUserId().equals(caller.userId());
		return this.mapper.toComment(comment, own, own || caller.admin());
	}

	private static String requireBody(String raw) {
		String body = raw == null ? "" : raw.trim();
		String message = body.isEmpty() ? TaskConstants.MESSAGE_COMMENT_REQUIRED
				: body.length() > TaskConstants.COMMENT_MAX_LENGTH ? TaskConstants.MESSAGE_COMMENT_TOO_LONG : null;
		if (message != null) {
			Map<String, Object> details = new HashMap<>();
			details.put(TaskConstants.DETAIL_BODY, message);
			throw new AppException(HttpStatus.BAD_REQUEST, AppConstants.ERROR_VALIDATION_FAILED,
					AppConstants.MESSAGE_VALIDATION_FAILED, details);
		}
		return body;
	}

	private static AppException forbidden() {
		return new AppException(HttpStatus.FORBIDDEN, TaskConstants.ERROR_FORBIDDEN,
				TaskConstants.MESSAGE_COMMENT_FORBIDDEN);
	}

}
