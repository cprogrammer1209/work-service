package com.projecty.work.common.service.impl;

import java.util.Optional;
import java.util.UUID;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.exception.AppException;
import com.projecty.work.common.service.RequestPortalContext;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Reads the portal id that {@code PortalContextFilter} stored on the current request.
 */
@Service
public class RequestPortalContextImpl implements RequestPortalContext {

	@Override
	public Optional<UUID> currentPortalId() {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		if (attributes == null) {
			return Optional.empty();
		}
		return Optional.ofNullable((UUID) attributes.getAttribute(AppConstants.REQUEST_ATTRIBUTE_PORTAL_ID, RequestAttributes.SCOPE_REQUEST));
	}

	@Override
	public UUID requirePortalId() {
		return currentPortalId().orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST,
				AppConstants.ERROR_PORTAL_REQUIRED, AppConstants.MESSAGE_PORTAL_REQUIRED));
	}

}
