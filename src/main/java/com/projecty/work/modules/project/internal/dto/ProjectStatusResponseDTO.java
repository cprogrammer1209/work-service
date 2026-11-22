package com.projecty.work.modules.project.internal.dto;

/**
 * @param type Active, On hold, Completed or Cancelled
 */
public record ProjectStatusResponseDTO(String name, String type) {
}
