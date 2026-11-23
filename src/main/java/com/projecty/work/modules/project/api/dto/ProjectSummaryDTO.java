package com.projecty.work.modules.project.api.dto;

import java.util.UUID;

/**
 * The few things other modules need to know about a project. {@code taskLayoutId} is the TASK layout the project's
 * tasks use, or null for the portal's default TASK layout.
 */
public record ProjectSummaryDTO(UUID id, String key, String name, UUID taskLayoutId) {
}
