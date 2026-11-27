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
import com.projecty.work.modules.access.internal.helper.MemberAccessCache;
import com.projecty.work.modules.access.internal.helper.MemberAccessCache.Access;
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

	private final MemberAccessCache accessCache;

	private final Clock clock;

	public MemberAccessServiceImpl(MemberAccessRepository accessRepository,
			ProcessedEventRepository processedEventRepository, MemberAccessCache accessCache, Clock clock) {
		this.accessRepository = accessRepository;
		this.processedEventRepository = processedEventRepository;
		this.accessCache = accessCache;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<MemberAccessResponseDTO> findMemberAccess(UUID portalId, UUID userId) {
		Access access = access(portalId, userId);
		return access.exists() ? Optional.of(new MemberAccessResponseDTO(portalId, userId, access.profileKey(),
				access.permissions(), access.active())) : Optional.empty();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isActiveMember(UUID portalId, UUID userId) {
		return access(portalId, userId).active();
	}

	@Override
	@Transactional(readOnly = true)
	public Set<String> permissions(UUID portalId, UUID userId) {
		Access access = access(portalId, userId);
		return access.active() ? access.permissions() : Set.of();
	}

	/** One cached answer serves all three reads, the two per-request checks and every service's own caller lookup. */
	private Access access(UUID portalId, UUID userId) {
		return this.accessCache.find(portalId, userId).orElseGet(() -> {
			Access loaded = load(portalId, userId);
			this.accessCache.put(portalId, userId, loaded);
			return loaded;
		});
	}

	private Access load(UUID portalId, UUID userId) {
		return this.accessRepository.findById(new MemberAccess.Key(portalId, userId))
			.map((access) -> new Access(true, access.isActive(), access.getProfileKey(),
					Set.copyOf(access.getPermissions())))
			.orElseGet(Access::none);
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
		this.accessCache.evictAfterCommit(event.portalId(), event.userId());
	}

}
