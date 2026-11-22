package com.projecty.work.modules.customfield.api;

import java.util.UUID;

/**
 * Implemented by each module that stores custom values in its own table. The customfield module cannot touch
 * another module's table, so to count or remove the answers of a field it asks the holder of that module.
 */
public interface CustomValueHolder {

	/** The module this holder serves, e.g. PROJECT. */
	String entityType();

	/** How many records of the portal have an answer for the key. */
	long countUsing(UUID portalId, String apiName);

	/** How many records of the portal use the layout. */
	long countUsingLayout(UUID portalId, UUID layoutId);

	/** Removes the key from every record of the portal. */
	void removeKey(UUID portalId, String apiName);

}
