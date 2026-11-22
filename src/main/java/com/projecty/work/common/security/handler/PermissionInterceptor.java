package com.projecty.work.common.security.handler;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.exception.AppException;
import com.projecty.work.common.security.annotation.RequirePermission;
import com.projecty.work.common.service.PortalAccessService;
import com.projecty.work.common.service.RequestPortalContext;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enforces {@link RequirePermission} before the controller runs (USR-US-06). Errors become AppResponse errors
 * through the global exception handler.
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

	private final RequestPortalContext portalContext;

	private final PortalAccessService portalAccessService;

	public PermissionInterceptor(RequestPortalContext portalContext, PortalAccessService portalAccessService) {
		this.portalContext = portalContext;
		this.portalAccessService = portalAccessService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!(handler instanceof HandlerMethod method)) {
			return true;
		}
		RequirePermission required = method.getMethodAnnotation(RequirePermission.class);
		if (required == null) {
			return true;
		}
		UUID portalId = this.portalContext.requirePortalId();
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UUID userId = UUID.fromString(authentication.getName());
		if (!this.portalAccessService.permissions(portalId, userId).contains(required.value())) {
			throw new AppException(HttpStatus.FORBIDDEN, AppConstants.ERROR_FORBIDDEN, AppConstants.MESSAGE_FORBIDDEN);
		}
		return true;
	}

}
