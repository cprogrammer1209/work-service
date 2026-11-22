package com.projecty.work.common.security.handler;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.projecty.work.common.constants.AppConstants;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 401 for a missing, malformed, expired or otherwise invalid access token.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final SecurityErrorWriter errorWriter;

	public RestAuthenticationEntryPoint(SecurityErrorWriter errorWriter) {
		this.errorWriter = errorWriter;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		this.errorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, AppConstants.ERROR_UNAUTHORIZED,
				AppConstants.MESSAGE_UNAUTHORIZED);
	}

}
