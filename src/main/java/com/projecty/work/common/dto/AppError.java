package com.projecty.work.common.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The error part of an {@link AppResponse}. {@code code} is stable and machine-readable (for example
 * {@code EMAIL_NOT_VERIFIED}); {@code details} carries extra data such as field errors or suggestions.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record AppError(String code, String message, Map<String, Object> details) {

	public static AppError of(String code, String message) {
		return new AppError(code, message, Map.of());
	}

}
