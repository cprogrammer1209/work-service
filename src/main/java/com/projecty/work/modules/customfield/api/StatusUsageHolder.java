package com.projecty.work.modules.customfield.api;

import java.util.UUID;

/**
 * Implemented by each module whose records carry a layout status (tasks today). The customfield module cannot read
 * another module's table, so before it deletes a status or changes its type it asks the holders.
 */
public interface StatusUsageHolder {

	/** How many records of the portal (trashed ones too) are in the status. */
	long countUsingStatus(UUID portalId, UUID statusId);

}
