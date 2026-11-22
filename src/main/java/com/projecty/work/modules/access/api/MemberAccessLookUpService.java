package com.projecty.work.modules.access.api;

import java.util.Optional;
import java.util.UUID;

import com.projecty.work.modules.access.api.dto.MemberAccessResponseDTO;

/**
 * What Work knows about a person in a portal: their profile, their permissions, and whether they are still
 * active. Other modules ask this instead of reading the table.
 */
public interface MemberAccessLookUpService {

	Optional<MemberAccessResponseDTO> findMemberAccess(UUID portalId, UUID userId);

}
