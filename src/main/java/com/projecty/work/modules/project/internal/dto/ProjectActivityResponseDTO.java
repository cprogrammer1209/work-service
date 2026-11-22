package com.projecty.work.modules.project.internal.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectActivityResponseDTO(String action, UUID actorUserId, String actorName, UUID targetUserId,
		String detail, Instant createdAt) {
}
