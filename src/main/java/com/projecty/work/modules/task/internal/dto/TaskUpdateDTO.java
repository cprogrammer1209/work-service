package com.projecty.work.modules.task.internal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.projecty.work.modules.task.internal.entity.TaskPriority;

/**
 * A partial update of a task: only the properties that are <b>in the request</b> change; a property left out stays as
 * it is. A {@code null} clears the description, dates, work hours, owners, tags, a custom answer, or makes the task
 * top-level ({@code parentId}); it is refused for the title, priority, completion, status and list.
 * <p>
 * A record cannot tell "left out" from "null", so this is a small class whose setters remember which properties were
 * set (Jackson only calls a setter for a property that is in the JSON). The limits are checked by the service, only
 * for what was sent.
 * <p>
 * {@code customValues}: only the keys sent change; a null or empty value clears that key. {@code statusId} is a status
 * of the task's layout; {@code listId} moves the task and its subtasks; {@code parentId} moves the task (with its
 * subtasks) under another task of the project.
 */
public class TaskUpdateDTO {

	private String title;

	private boolean titleSet;

	private String description;

	private boolean descriptionSet;

	private TaskPriority priority;

	private boolean prioritySet;

	private LocalDate startDate;

	private boolean startDateSet;

	private LocalDate dueDate;

	private boolean dueDateSet;

	private BigDecimal workHours;

	private boolean workHoursSet;

	private Integer completion;

	private boolean completionSet;

	private List<UUID> ownerIds;

	private boolean ownerIdsSet;

	private List<String> tags;

	private boolean tagsSet;

	private UUID statusId;

	private boolean statusIdSet;

	private UUID listId;

	private boolean listIdSet;

	private UUID parentId;

	private boolean parentIdSet;

	private Map<String, Object> customValues;

	private boolean customValuesSet;

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
		this.titleSet = true;
	}

	public boolean isTitleSet() {
		return this.titleSet;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
		this.descriptionSet = true;
	}

	public boolean isDescriptionSet() {
		return this.descriptionSet;
	}

	public TaskPriority getPriority() {
		return this.priority;
	}

	public void setPriority(TaskPriority priority) {
		this.priority = priority;
		this.prioritySet = true;
	}

	public boolean isPrioritySet() {
		return this.prioritySet;
	}

	public LocalDate getStartDate() {
		return this.startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
		this.startDateSet = true;
	}

	public boolean isStartDateSet() {
		return this.startDateSet;
	}

	public LocalDate getDueDate() {
		return this.dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
		this.dueDateSet = true;
	}

	public boolean isDueDateSet() {
		return this.dueDateSet;
	}

	public BigDecimal getWorkHours() {
		return this.workHours;
	}

	public void setWorkHours(BigDecimal workHours) {
		this.workHours = workHours;
		this.workHoursSet = true;
	}

	public boolean isWorkHoursSet() {
		return this.workHoursSet;
	}

	public Integer getCompletion() {
		return this.completion;
	}

	public void setCompletion(Integer completion) {
		this.completion = completion;
		this.completionSet = true;
	}

	public boolean isCompletionSet() {
		return this.completionSet;
	}

	public List<UUID> getOwnerIds() {
		return this.ownerIds;
	}

	public void setOwnerIds(List<UUID> ownerIds) {
		this.ownerIds = ownerIds;
		this.ownerIdsSet = true;
	}

	public boolean isOwnerIdsSet() {
		return this.ownerIdsSet;
	}

	public List<String> getTags() {
		return this.tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
		this.tagsSet = true;
	}

	public boolean isTagsSet() {
		return this.tagsSet;
	}

	public UUID getStatusId() {
		return this.statusId;
	}

	public void setStatusId(UUID statusId) {
		this.statusId = statusId;
		this.statusIdSet = true;
	}

	public boolean isStatusIdSet() {
		return this.statusIdSet;
	}

	public UUID getListId() {
		return this.listId;
	}

	public void setListId(UUID listId) {
		this.listId = listId;
		this.listIdSet = true;
	}

	public boolean isListIdSet() {
		return this.listIdSet;
	}

	public UUID getParentId() {
		return this.parentId;
	}

	public void setParentId(UUID parentId) {
		this.parentId = parentId;
		this.parentIdSet = true;
	}

	public boolean isParentIdSet() {
		return this.parentIdSet;
	}

	public Map<String, Object> getCustomValues() {
		return this.customValues;
	}

	public void setCustomValues(Map<String, Object> customValues) {
		this.customValues = customValues;
		this.customValuesSet = true;
	}

	public boolean isCustomValuesSet() {
		return this.customValuesSet;
	}

}
