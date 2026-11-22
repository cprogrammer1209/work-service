package com.projecty.work.modules.access.internal.service;

import com.projecty.work.common.service.PortalAccessService;
import com.projecty.work.modules.access.api.MemberAccessLookUpService;
import com.projecty.work.modules.access.internal.dto.MemberEventDTO;

/**
 * The full access service: lookups for other modules, the portal-access port the security filter needs, and the
 * event handling that keeps the copy current.
 */
public interface MemberAccessService extends MemberAccessLookUpService, PortalAccessService {

	/**
	 * Applies one Platform member event. A repeated event (same id) changes nothing.
	 */
	void apply(MemberEventDTO event);

}
