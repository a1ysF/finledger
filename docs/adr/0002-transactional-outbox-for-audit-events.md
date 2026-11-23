# Transactional outbox for audit events

Every Transfer, completed or rejected, writes its event to an `outbox_events` row in the same database transaction as the money; a separate relay publishes those rows to Kafka. Calling Kafka directly from the Transfer code is a dual write: if the app crashes between the database commit and the Kafka send, money moves with no event, or an event exists for money that never moved. With the outbox, the event exists if and only if the Transfer committed, and Transfers keep working while Kafka is down.

## Consequences

- Delivery is at-least-once (the relay can crash after sending but before marking a row published), so the audit consumer must ignore duplicate `event_id`s.
- The audit log lags the ledger by the relay's polling interval; reconciliation, not the audit log, is the real-time check.
