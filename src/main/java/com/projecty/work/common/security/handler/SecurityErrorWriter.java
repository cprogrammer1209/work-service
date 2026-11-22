package com.projecty.work.common.security.handler;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import com.projecty.work.common.dto.AppError;
import com.projecty.work.common.dto.AppResponse;
import tools.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Writes security failures (401/403) as {@link AppResponse} errors. These happen in the filter chain, before
 * any controller, so the global exception handler never sees them.
 */
@Component
public class SecurityErrorWriter {

	private final ObjectMapper objectMapper;

	public SecurityErrorWriter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void write(HttpServletResponse response, int status, String code, String message) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		this.objectMapper.writeValue(response.getOutputStream(), AppResponse.error(AppError.of(code, message)));
	}

}
