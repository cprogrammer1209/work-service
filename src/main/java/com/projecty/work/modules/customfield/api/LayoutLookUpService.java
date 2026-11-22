package com.projecty.work.modules.customfield.api;

import java.util.List;
import java.util.UUID;

import com.projecty.work.modules.customfield.api.dto.LayoutResponseDTO;

/**
 * The layouts of a module, for the modules that draw or validate records (today: projects). A module that has no
 * layout yet gets its default layout created the first time it is asked.
 */
public interface LayoutLookUpService {

	/** The active layouts of a module, default first. */
	List<LayoutResponseDTO> findActiveLayouts(UUID portalId, String entityType);

	/** One layout (active or not), or a "not found" error when it is not in the portal. */
	LayoutResponseDTO getLayout(UUID portalId, UUID layoutId);

	/** The id of the module's default layout. */
	UUID getDefaultLayoutId(UUID portalId, String entityType);

}
