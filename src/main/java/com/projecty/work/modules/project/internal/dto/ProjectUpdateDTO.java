package com.projecty.work.modules.project.internal.dto;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.projecty.work.modules.project.internal.entity.ProjectVisibility;

/**
 * A partial update of a project: only the properties that are <b>in the request</b> change; a property that is left
 * out stays as it is, and a {@code null} clears the start date, end date, description or an answer. The complete Edit
 * form sends every property, a single-field edit sends one. The key is not here: it never changes (PRJ-BR-01), and
 * a null owner keeps the owner.
 * <p>
 * A record cannot tell "left out" from "null", so this is a small class whose setters remember which properties were
 * set (Jackson only calls a setter for a property that is in the JSON). The limits (name, description, visibility)
 * are checked by the service, only for what was sent.
 * <p>
 * {@code customValues}: only the keys sent change; a null or empty value clears that key.
 */
public class ProjectUpdateDTO {

	private String name;

	private boolean nameSet;

	private String description;

	private boolean descriptionSet;

	private UUID ownerUserId;

	private boolean ownerUserIdSet;

	private LocalDate startDate;

	private boolean startDateSet;

	private LocalDate endDate;

	private boolean endDateSet;

	private ProjectVisibility visibility;

	private boolean visibilitySet;

	private Map<String, Object> customValues;

	private boolean customValuesSet;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
		this.nameSet = true;
	}

	public boolean isNameSet() {
		return this.nameSet;
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

	public UUID getOwnerUserId() {
		return this.ownerUserId;
	}

	public void setOwnerUserId(UUID ownerUserId) {
		this.ownerUserId = ownerUserId;
		this.ownerUserIdSet = true;
	}

	public boolean isOwnerUserIdSet() {
		return this.ownerUserIdSet;
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

	public LocalDate getEndDate() {
		return this.endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
		this.endDateSet = true;
	}

	public boolean isEndDateSet() {
		return this.endDateSet;
	}

	public ProjectVisibility getVisibility() {
		return this.visibility;
	}

	public void setVisibility(ProjectVisibility visibility) {
		this.visibility = visibility;
		this.visibilitySet = true;
	}

	public boolean isVisibilitySet() {
		return this.visibilitySet;
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
