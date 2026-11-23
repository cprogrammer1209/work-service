package com.projecty.work.modules.task.internal.helper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.exception.AppException;
import com.projecty.work.modules.task.internal.constants.TaskConstants;
import com.projecty.work.modules.task.internal.entity.TaskPriority;

import org.springframework.http.HttpStatus;

/**
 * Checks and cleans the values of one task request. It collects every problem and {@link #verify()} then answers
 * VALIDATION_FAILED with all of them under {@code details}, so a form can show every message at once. Make one per
 * request.
 */
public class TaskInputChecker {

	private final Map<String, Object> problems = new LinkedHashMap<>();

	public String title(String raw) {
		String title = raw == null ? "" : raw.trim();
		if (title.isEmpty()) {
			problem(TaskConstants.DETAIL_TITLE, TaskConstants.MESSAGE_TITLE_REQUIRED);
		}
		else if (title.length() > TaskConstants.TITLE_MAX_LENGTH) {
			problem(TaskConstants.DETAIL_TITLE, TaskConstants.MESSAGE_TITLE_TOO_LONG);
		}
		return title;
	}

	/** Blank becomes null. */
	public String description(String raw) {
		String description = raw == null || raw.isBlank() ? null : raw.trim();
		if (description != null && description.length() > TaskConstants.DESCRIPTION_MAX_LENGTH) {
			problem(TaskConstants.DETAIL_DESCRIPTION, TaskConstants.MESSAGE_DESCRIPTION_TOO_LONG);
		}
		return description;
	}

	public TaskPriority priority(TaskPriority priority) {
		if (priority == null) {
			problem(TaskConstants.DETAIL_PRIORITY, TaskConstants.MESSAGE_PRIORITY_REQUIRED);
			return TaskPriority.NONE;
		}
		return priority;
	}

	public void dates(LocalDate start, LocalDate due) {
		if (start != null && due != null && due.isBefore(start)) {
			problem(TaskConstants.DETAIL_DUE_DATE, TaskConstants.MESSAGE_DATES_INVALID);
		}
	}

	/** Null is allowed (no estimate); otherwise 0 to 999999 with at most 2 decimals. */
	public BigDecimal workHours(BigDecimal hours) {
		if (hours == null) {
			return null;
		}
		BigDecimal stripped = hours.stripTrailingZeros();
		int scale = Math.max(stripped.scale(), 0);
		if (hours.signum() < 0 || hours.compareTo(BigDecimal.valueOf(TaskConstants.WORK_HOURS_MAX)) > 0
				|| scale > TaskConstants.WORK_HOURS_SCALE) {
			problem(TaskConstants.DETAIL_WORK_HOURS, TaskConstants.MESSAGE_WORK_HOURS_INVALID);
			return null;
		}
		return hours.setScale(TaskConstants.WORK_HOURS_SCALE, java.math.RoundingMode.UNNECESSARY);
	}

	public int completion(Integer value) {
		if (value == null || value < 0 || value > TaskConstants.COMPLETION_MAX) {
			problem(TaskConstants.DETAIL_COMPLETION, TaskConstants.MESSAGE_COMPLETION_INVALID);
			return 0;
		}
		return value;
	}

	/** Trimmed, blank ones refused, case-insensitive duplicates dropped (the first spelling stays). */
	public List<String> tags(List<String> raw) {
		List<String> tags = new ArrayList<>();
		if (raw == null) {
			return tags;
		}
		Set<String> seen = new LinkedHashSet<>();
		for (String tag : raw) {
			String clean = tag == null ? "" : tag.trim();
			if (clean.isEmpty() || clean.length() > TaskConstants.TAG_MAX_LENGTH) {
				problem(TaskConstants.DETAIL_TAGS, TaskConstants.MESSAGE_TAG_INVALID);
			}
			else if (seen.add(clean.toLowerCase())) {
				tags.add(clean);
			}
		}
		if (tags.size() > TaskConstants.TAG_LIMIT) {
			problem(TaskConstants.DETAIL_TAGS, TaskConstants.MESSAGE_TAGS_TOO_MANY);
		}
		return tags;
	}

	/** Duplicates dropped, order kept. */
	public List<UUID> owners(List<UUID> raw) {
		List<UUID> owners = raw == null ? new ArrayList<>()
				: new ArrayList<>(new LinkedHashSet<>(raw.stream().filter(java.util.Objects::nonNull).toList()));
		if (owners.size() > TaskConstants.OWNER_LIMIT) {
			problem(TaskConstants.DETAIL_OWNERS, TaskConstants.MESSAGE_OWNERS_TOO_MANY);
		}
		return owners;
	}

	public void problem(String property, String message) {
		this.problems.putIfAbsent(property, message);
	}

	public void verify() {
		if (!this.problems.isEmpty()) {
			throw new AppException(HttpStatus.BAD_REQUEST, AppConstants.ERROR_VALIDATION_FAILED,
					AppConstants.MESSAGE_VALIDATION_FAILED, new LinkedHashMap<>(this.problems));
		}
	}

}
