package com.projecty.work.modules.outbox.internal.repository;

import java.util.List;
import java.util.UUID;

import com.projecty.work.modules.outbox.internal.entity.OutboxEvent;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

	/**
	 * The oldest unpublished events, locked so that a second relay instance skips them instead of
	 * publishing them twice at the same time. Oldest first; the id only breaks ties.
	 */
	@Query(value = "SELECT * FROM outbox_event WHERE published_at IS NULL ORDER BY created_at, id LIMIT :limit "
			+ "FOR UPDATE SKIP LOCKED", nativeQuery = true)
	List<OutboxEvent> lockOldestUnpublished(@Param("limit") int limit);

	@Modifying
	@Query("DELETE FROM OutboxEvent e WHERE e.publishedAt < :before")
	int deletePublishedBefore(@Param("before") Instant before);

}
