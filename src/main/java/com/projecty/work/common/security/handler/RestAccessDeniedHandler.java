package com.projecty.work.common.security.handler;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.projecty.work.common.constants.AppConstants;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 403 for an authenticated caller who isn't allowed to perform the request.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	private final SecurityErrorWriter errorWriter;

	public RestAccessDeniedHandler(SecurityErrorWriter errorWriter) {
		this.errorWriter = errorWriter;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		this.errorWriter.write(response, HttpServletResponse.SC_FORBIDDEN, AppConstants.ERROR_FORBIDDEN,
				AppConstants.MESSAGE_FORBIDDEN);
	}

}
