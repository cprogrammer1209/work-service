package com.projecty.work.modules.task.internal.helper;

import java.util.Set;
import java.util.UUID;

/** Who is asking: their profile and permissions in the portal, from Work's copy of Platform's member events. */
public record TaskCaller(UUID userId, boolean admin, boolean client, Set<String> permissions) {

	public boolean can(String permission) {
		return this.permissions.contains(permission);
	}

}
