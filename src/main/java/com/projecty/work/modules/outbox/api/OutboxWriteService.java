package com.projecty.work.modules.outbox.api;

import com.projecty.work.modules.outbox.api.dto.OutboxEventCreateDTO;

public interface OutboxWriteService {

	/**
	 * Appends a business event to the outbox. Must run inside the transaction of the change it describes, so a
	 * rollback removes it too; a relay publishes it after commit, at least once.
	 * @throws org.springframework.transaction.IllegalTransactionStateException when there is no transaction
	 */
	void publish(OutboxEventCreateDTO event);

}
