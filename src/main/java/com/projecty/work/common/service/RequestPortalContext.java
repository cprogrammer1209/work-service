package com.projecty.work.common.service;

import java.util.Optional;
import java.util.UUID;

/**
 * The portal the current customer request acts in, taken from the {@code X-Portal-Id} header after
 * {@code PortalContextFilter} checked it against the caller's token. Portal-owned data is always read and
 * written with this id, never one from the request body (PTL-BR-01).
 */
public interface RequestPortalContext {

	Optional<UUID> currentPortalId();

	/**
	 * @throws com.projecty.work.common.exception.AppException 400 {@code PORTAL_REQUIRED} without a portal
	 */
	UUID requirePortalId();

}
