-- Preserve legacy audit history while making it clear that new decisions belong to approval-service.
ALTER TABLE payment_approval_decisions
	RENAME TO legacy_payment_approval_decisions;
