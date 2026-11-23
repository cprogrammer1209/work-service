package com.projecty.work.modules.customfield.internal.service.impl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.exception.AppException;
import com.projecty.work.common.service.RequestPortalContext;
import com.projecty.work.modules.customfield.api.StatusUsageHolder;
import com.projecty.work.modules.customfield.api.dto.LayoutStatusResponseDTO;
import com.projecty.work.modules.customfield.internal.constants.CustomFieldConstants;
import com.projecty.work.modules.customfield.internal.dto.LayoutStatusCreateDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutStatusOrderDTO;
import com.projecty.work.modules.customfield.internal.dto.LayoutStatusUpdateDTO;
import com.projecty.work.modules.customfield.internal.entity.Layout;
import com.projecty.work.modules.customfield.internal.entity.LayoutStatus;
import com.projecty.work.modules.customfield.internal.entity.LayoutStatusType;
import com.projecty.work.modules.customfield.internal.mapper.LayoutMapper;
import com.projecty.work.modules.customfield.internal.repository.LayoutRepository;
import com.projecty.work.modules.customfield.internal.repository.LayoutStatusRepository;
import com.projecty.work.modules.customfield.internal.service.LayoutStatusService;
import com.projecty.work.modules.outbox.api.OutboxWriteService;
import com.projecty.work.modules.outbox.api.dto.OutboxEventCreateDTO;
import com.projecty.work.modules.shared.constants.OutboxEventConstants;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LayoutStatusServiceImpl implements LayoutStatusService {

	private final LayoutRepository layoutRepository;

	private final LayoutStatusRepository statusRepository;

	private final List<StatusUsageHolder> usageHolders;

	private final LayoutMapper mapper;

	private final OutboxWriteService outboxWriteService;

	private final RequestPortalContext portalContext;

	public LayoutStatusServiceImpl(LayoutRepository layoutRepository, LayoutStatusRepository statusRepository,
			List<StatusUsageHolder> usageHolders, LayoutMapper mapper, OutboxWriteService outboxWriteService,
			RequestPortalContext portalContext) {
		this.layoutRepository = layoutRepository;
		this.statusRepository = statusRepository;
		this.usageHolders = usageHolders;
		this.mapper = mapper;
		this.outboxWriteService = outboxWriteService;
		this.portalContext = portalContext;
	}

	@Override
	@Transactional(readOnly = true)
	public List<LayoutStatusResponseDTO> list(UUID layoutId) {
		UUID portalId = this.portalContext.requirePortalId();
		Layout layout = this.layoutRepository.findByIdAndPortalId(layoutId, portalId).orElseThrow(this::layoutNotFound);
		requireTaskLayout(layout);
		return respond(portalId, layoutId);
	}

	@Override
	@Transactional
	public List<LayoutStatusResponseDTO> create(UUID actorId, String actorName, UUID layoutId,
			LayoutStatusCreateDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		Layout layout = lockTaskLayout(portalId, layoutId);
		List<LayoutStatus> statuses = this.statusRepository.findByPortalIdAndLayoutIdOrderBySortOrderAscIdAsc(portalId, layoutId);
		if (statuses.size() >= CustomFieldConstants.STATUS_LIMIT) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, CustomFieldConstants.ERROR_STATUS_LIMIT,
					CustomFieldConstants.MESSAGE_STATUS_LIMIT);
		}
		String name = requireName(request.name(), statuses, null);
		int order = statuses.stream().mapToInt(LayoutStatus::getSortOrder).max().orElse(-1) + 1;
		this.statusRepository.save(new LayoutStatus(portalId, layoutId, name, request.statusType(),
				cleanColor(request.color()), order, false));
		this.statusRepository.flush();
		publish(layout, actorId, actorName);
		return respond(portalId, layoutId);
	}

	@Override
	@Transactional
	public List<LayoutStatusResponseDTO> update(UUID actorId, String actorName, UUID layoutId, UUID statusId,
			LayoutStatusUpdateDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		Layout layout = lockTaskLayout(portalId, layoutId);
		List<LayoutStatus> statuses = this.statusRepository.findByPortalIdAndLayoutIdOrderBySortOrderAscIdAsc(portalId, layoutId);
		LayoutStatus status = statuses.stream()
			.filter((candidate) -> candidate.getId().equals(statusId))
			.findFirst()
			.orElseThrow(this::statusNotFound);
		String name = request.name() == null ? status.getName() : requireName(request.name(), statuses, statusId);
		String color = request.color() == null ? status.getColor() : cleanColor(request.color());
		LayoutStatusType type = request.statusType() == null ? status.getStatusType() : request.statusType();
		if (type != status.getStatusType()) {
			if (status.isDefaultStatus()) {
				throw invalid(CustomFieldConstants.DETAIL_STATUS_TYPE, CustomFieldConstants.MESSAGE_STATUS_DEFAULT_OPEN);
			}
			long ofOldType = statuses.stream().filter((s) -> s.getStatusType() == status.getStatusType()).count();
			if (ofOldType <= 1) {
				throw invalid(CustomFieldConstants.DETAIL_STATUS_TYPE, CustomFieldConstants.MESSAGE_STATUS_LAST_OF_TYPE);
			}
			if (usage(portalId, statusId) > 0) {
				throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, CustomFieldConstants.ERROR_STATUS_IN_USE,
						CustomFieldConstants.MESSAGE_STATUS_TYPE_IN_USE);
			}
		}
		boolean becomesDefault = Boolean.TRUE.equals(request.defaultStatus()) && !status.isDefaultStatus();
		if (Boolean.FALSE.equals(request.defaultStatus()) && status.isDefaultStatus()) {
			throw invalid(CustomFieldConstants.DETAIL_STATUS_DEFAULT, CustomFieldConstants.MESSAGE_STATUS_DEFAULT_UNSET);
		}
		if (becomesDefault) {
			if (type != LayoutStatusType.OPEN) {
				throw invalid(CustomFieldConstants.DETAIL_STATUS_TYPE, CustomFieldConstants.MESSAGE_STATUS_DEFAULT_OPEN);
			}
			// The unique index allows one default, so the old one is cleared (and flushed) first.
			statuses.stream().filter(LayoutStatus::isDefaultStatus).forEach((s) -> s.changeDefault(false));
			this.statusRepository.flush();
			status.changeDefault(true);
		}
		status.edit(name, type, color);
		this.statusRepository.flush();
		publish(layout, actorId, actorName);
		return respond(portalId, layoutId);
	}

	@Override
	@Transactional
	public List<LayoutStatusResponseDTO> delete(UUID actorId, String actorName, UUID layoutId, UUID statusId) {
		UUID portalId = this.portalContext.requirePortalId();
		Layout layout = lockTaskLayout(portalId, layoutId);
		List<LayoutStatus> statuses = this.statusRepository.findByPortalIdAndLayoutIdOrderBySortOrderAscIdAsc(portalId, layoutId);
		LayoutStatus status = statuses.stream()
			.filter((candidate) -> candidate.getId().equals(statusId))
			.findFirst()
			.orElseThrow(this::statusNotFound);
		if (status.isDefaultStatus()) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, CustomFieldConstants.ERROR_STATUS_INVALID,
					CustomFieldConstants.MESSAGE_STATUS_DEFAULT_UNSET);
		}
		if (statuses.stream().filter((s) -> s.getStatusType() == status.getStatusType()).count() <= 1) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, CustomFieldConstants.ERROR_STATUS_INVALID,
					CustomFieldConstants.MESSAGE_STATUS_LAST_OF_TYPE);
		}
		if (usage(portalId, statusId) > 0) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, CustomFieldConstants.ERROR_STATUS_IN_USE,
					CustomFieldConstants.MESSAGE_STATUS_IN_USE);
		}
		this.statusRepository.delete(status);
		this.statusRepository.flush();
		publish(layout, actorId, actorName);
		return respond(portalId, layoutId);
	}

	@Override
	@Transactional
	public List<LayoutStatusResponseDTO> reorder(UUID actorId, String actorName, UUID layoutId,
			LayoutStatusOrderDTO request) {
		UUID portalId = this.portalContext.requirePortalId();
		Layout layout = lockTaskLayout(portalId, layoutId);
		Map<UUID, LayoutStatus> statuses = this.statusRepository
			.findByPortalIdAndLayoutIdOrderBySortOrderAscIdAsc(portalId, layoutId)
			.stream()
			.collect(Collectors.toMap(LayoutStatus::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
		Set<UUID> distinct = Set.copyOf(request.ids());
		if (distinct.size() != request.ids().size() || !statuses.keySet().containsAll(distinct)) {
			throw invalid(CustomFieldConstants.DETAIL_STATUS_IDS, CustomFieldConstants.MESSAGE_STATUS_ORDER_INVALID);
		}
		// Listed statuses first in that order, the rest follow in their current order.
		int order = 0;
		for (UUID id : request.ids()) {
			statuses.get(id).changeSortOrder(order++);
		}
		for (LayoutStatus status : statuses.values()) {
			if (!distinct.contains(status.getId())) {
				status.changeSortOrder(order++);
			}
		}
		this.statusRepository.flush();
		publish(layout, actorId, actorName);
		return respond(portalId, layoutId);
	}

	/* ---------- helpers ---------- */

	private Layout lockTaskLayout(UUID portalId, UUID layoutId) {
		Layout layout = this.layoutRepository.lockByIdAndPortalId(layoutId, portalId).orElseThrow(this::layoutNotFound);
		requireTaskLayout(layout);
		return layout;
	}

	private static void requireTaskLayout(Layout layout) {
		if (!CustomFieldConstants.ENTITY_TASK.equals(layout.getEntityType())) {
			throw new AppException(HttpStatus.UNPROCESSABLE_CONTENT, CustomFieldConstants.ERROR_LAYOUT_INVALID,
					CustomFieldConstants.MESSAGE_STATUSES_UNSUPPORTED);
		}
	}

	private List<LayoutStatusResponseDTO> respond(UUID portalId, UUID layoutId) {
		return this.statusRepository.findByPortalIdAndLayoutIdOrderBySortOrderAscIdAsc(portalId, layoutId)
			.stream()
			.map(this.mapper::toStatus)
			.toList();
	}

	private long usage(UUID portalId, UUID statusId) {
		return this.usageHolders.stream().mapToLong((holder) -> holder.countUsingStatus(portalId, statusId)).sum();
	}

	private static String requireName(String raw, List<LayoutStatus> existing, UUID selfId) {
		String name = raw == null ? "" : raw.trim();
		if (name.isEmpty()) {
			throw invalid(CustomFieldConstants.DETAIL_STATUS_NAME, CustomFieldConstants.MESSAGE_STATUS_NAME_REQUIRED);
		}
		if (name.length() > CustomFieldConstants.STATUS_NAME_MAX_LENGTH) {
			throw invalid(CustomFieldConstants.DETAIL_STATUS_NAME, "Use " + CustomFieldConstants.STATUS_NAME_MAX_LENGTH + " characters or fewer.");
		}
		if (existing.stream().anyMatch((s) -> !s.getId().equals(selfId) && s.getName().equalsIgnoreCase(name))) {
			throw invalid(CustomFieldConstants.DETAIL_STATUS_NAME, CustomFieldConstants.MESSAGE_STATUS_NAME_TAKEN);
		}
		return name;
	}

	private static String cleanColor(String color) {
		return color == null || color.isBlank() ? null : color.trim();
	}

	private static AppException invalid(String detail, String message) {
		Map<String, Object> details = new HashMap<>();
		details.put(detail, message);
		return new AppException(HttpStatus.BAD_REQUEST, AppConstants.ERROR_VALIDATION_FAILED,
				AppConstants.MESSAGE_VALIDATION_FAILED, details);
	}

	private AppException layoutNotFound() {
		return new AppException(HttpStatus.NOT_FOUND, CustomFieldConstants.ERROR_LAYOUT_NOT_FOUND,
				CustomFieldConstants.MESSAGE_LAYOUT_NOT_FOUND);
	}

	private AppException statusNotFound() {
		return new AppException(HttpStatus.NOT_FOUND, CustomFieldConstants.ERROR_STATUS_NOT_FOUND,
				CustomFieldConstants.MESSAGE_STATUS_NOT_FOUND);
	}

	/** Same transaction as the change; a status edit is a change of the layout. */
	private void publish(Layout layout, UUID actorId, String actorName) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("portalId", layout.getPortalId());
		payload.put("layoutId", layout.getId());
		payload.put("entityType", layout.getEntityType());
		payload.put("change", "statuses");
		this.outboxWriteService.publish(new OutboxEventCreateDTO(layout.getPortalId(),
				OutboxEventConstants.AGGREGATE_LAYOUT, layout.getId(), OutboxEventConstants.EVENT_LAYOUT_UPDATED,
				actorId, actorName, payload));
	}

}
