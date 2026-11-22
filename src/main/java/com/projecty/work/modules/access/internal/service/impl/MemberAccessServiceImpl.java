package com.projecty.work.modules.access.internal.service.impl;

import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.projecty.work.modules.access.api.dto.MemberAccessResponseDTO;
import com.projecty.work.modules.access.internal.constants.AccessConstants;
import com.projecty.work.modules.access.internal.dto.MemberEventDTO;
import com.projecty.work.modules.access.internal.entity.MemberAccess;
import com.projecty.work.modules.access.internal.entity.ProcessedEvent;
import com.projecty.work.modules.access.internal.mapper.MemberAccessMapper;
import com.projecty.work.modules.access.internal.repository.MemberAccessRepository;
import com.projecty.work.modules.access.internal.repository.ProcessedEventRepository;
import com.projecty.work.modules.access.internal.service.MemberAccessService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberAccessServiceImpl implements MemberAccessService {

	private static final Logger log = LoggerFactory.getLogger(MemberAccessServiceImpl.class);

	private final MemberAccessRepository accessRepository;

	private final ProcessedEventRepository processedEventRepository;

	private final MemberAccessMapper mapper;

	private final Clock clock;

	public MemberAccessServiceImpl(MemberAccessRepository accessRepository,
			ProcessedEventRepository processedEventRepository, MemberAccessMapper mapper, Clock clock) {
		this.accessRepository = accessRepository;
		this.processedEventRepository = processedEventRepository;
		this.mapper = mapper;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<MemberAccessResponseDTO> findMemberAccess(UUID portalId, UUID userId) {
		return this.accessRepository.findById(new MemberAccess.Key(portalId, userId)).map(this.mapper::toResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isActiveMember(UUID portalId, UUID userId) {
		return this.accessRepository.findById(new MemberAccess.Key(portalId, userId))
			.map(MemberAccess::isActive)
			.orElse(false);
	}

	@Override
	@Transactional(readOnly = true)
	public Set<String> permissions(UUID portalId, UUID userId) {
		return this.accessRepository.findById(new MemberAccess.Key(portalId, userId))
			.filter(MemberAccess::isActive)
			.map((access) -> Set.copyOf(access.getPermissions()))
			.orElse(Set.of());
	}

	@Override
	@Transactional
	public void apply(MemberEventDTO event) {
		if (this.processedEventRepository.existsById(event.eventId())) {
			return;
		}
		var now = this.clock.instant();
		MemberAccess.Key key = new MemberAccess.Key(event.portalId(), event.userId());
		switch (event.type()) {
			case AccessConstants.EVENT_USER_ACTIVATED -> this.accessRepository.save(this.accessRepository.findById(key)
				.orElseGet(() -> new MemberAccess(event.portalId(), event.userId(), now)))
				.activate(event.profileKey(), event.permissions(), now);
			case AccessConstants.EVENT_USER_PROFILE_CHANGED -> this.accessRepository.findById(key)
				.ifPresentOrElse((access) -> access.change(event.profileKey(), event.permissions(), now),
						() -> log.warn("Profile change for an unknown member {} in portal {}", event.userId(),
								event.portalId()));
			case AccessConstants.EVENT_USER_DEACTIVATED -> this.accessRepository.findById(key)
				.ifPresent((access) -> access.deactivate(now));
			default -> {
				// Other member events do not change access.
			}
		}
		this.processedEventRepository.save(new ProcessedEvent(event.eventId(), now));
	}

}
