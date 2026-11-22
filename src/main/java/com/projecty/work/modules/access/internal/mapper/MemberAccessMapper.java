package com.projecty.work.modules.access.internal.mapper;

import java.util.Set;

import com.projecty.work.modules.access.api.dto.MemberAccessResponseDTO;
import com.projecty.work.modules.access.internal.entity.MemberAccess;

import org.springframework.stereotype.Component;

@Component
public class MemberAccessMapper {

	public MemberAccessResponseDTO toResponse(MemberAccess access) {
		return new MemberAccessResponseDTO(access.getPortalId(), access.getUserId(), access.getProfileKey(),
				Set.copyOf(access.getPermissions()), access.isActive());
	}

}
