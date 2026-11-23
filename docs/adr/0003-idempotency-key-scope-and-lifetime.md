# Idempotency Keys are per client, permanent, and used up by a rejection

An Idempotency Key is unique per client (the `SERVICE` client, or the ops user for Reversals) across every money endpoint, never expires, and is used up by a Rejected Transfer as well as a Completed one: a retry always returns the original result. Requests that never reached a decision (validation `400`, `ACCOUNT_NOT_FOUND`, `SAME_ACCOUNT`, `503 TRY_AGAIN`) store nothing and leave the key free. When two requests with the same key race, the loser returns the winner's stored result as a normal replay.

## Considered Options

- **Free the key after a rejection**: rejected; a client that already showed "insufficient funds" to a customer could have a later retry silently complete the Transfer.
- **Globally unique keys**: rejected; two clients choosing the same key would get a confusing "different body" error and learn that another client used it.
- **Keys that expire after N hours**: rejected; the Transfer row holding the key is permanent anyway, and expiry would make a late retry move money twice.
