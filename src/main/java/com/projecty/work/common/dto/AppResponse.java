package com.projecty.work.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The envelope for every API response: {@code {"success": true, "data": ...}} or
 * {@code {"success": false, "error": {...}}}. Empty fields are left out.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AppResponse<T>(boolean success, T data, AppError error) {

	public static <T> AppResponse<T> ok(T data) {
		return new AppResponse<>(true, data, null);
	}

	public static AppResponse<Void> ok() {
		return new AppResponse<>(true, null, null);
	}

	public static AppResponse<Void> error(AppError error) {
		return new AppResponse<>(false, null, error);
	}

}
