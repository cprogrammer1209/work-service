package com.projecty.work.common.service;

import java.util.Set;
import java.util.UUID;

/**
 * Port for security checks that need live membership data. Defined here so common/ needn't import a module;
 * implemented by the member module.
 */
public interface PortalAccessService {

	/**
	 * Whether the user is an active member right now. Deactivation takes effect on the next request, even
	 * though the user's access token still lists the portal (USR-BR-09).
	 */
	boolean isActiveMember(UUID portalId, UUID userId);

	/**
	 * The permissions the user's profile grants in the portal; empty if they aren't an active member.
	 */
	Set<String> permissions(UUID portalId, UUID userId);

}
