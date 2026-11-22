package com.projecty.work.modules.outbox.internal.service;

import com.projecty.work.modules.outbox.api.OutboxWriteService;

public interface OutboxService extends OutboxWriteService {

	/**
	 * Publishes one batch of the oldest unpublished events, in order, and marks each published.
	 * Stops at the first failure so order is kept; the failed event is retried on the next run.
	 * @return how many events were published
	 */
	int relayBatch();

	/**
	 * Deletes events that were published longer ago than the retention.
	 * @return how many were deleted
	 */
	int purgePublished();

}
