package com.projecty.work.common.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * A business error. It becomes an {@code AppResponse} error with this status, code, message and details.
 */
public class AppException extends RuntimeException {

	private final HttpStatus status;

	private final String code;

	private final Map<String, Object> details;

	public AppException(HttpStatus status, String code, String message) {
		this(status, code, message, Map.of());
	}

	public AppException(HttpStatus status, String code, String message, Map<String, Object> details) {
		super(message);
		this.status = status;
		this.code = code;
		this.details = details;
	}

	public HttpStatus getStatus() {
		return this.status;
	}

	public String getCode() {
		return this.code;
	}

	public Map<String, Object> getDetails() {
		return this.details;
	}

}
