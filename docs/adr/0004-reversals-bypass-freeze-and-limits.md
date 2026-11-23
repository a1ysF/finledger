# Reversals ignore freezes and limits, but not insufficient funds

A Reversal is allowed even when either Account is Frozen, is exempt from the Per-Transfer Limit and the Daily Limit, and does not count toward the Daily Limit. It is still rejected with `INSUFFICIENT_FUNDS` if the account it takes money from no longer holds enough. The main use of a Reversal is fraud recovery: ops freezes the fraudster's account, then reverses the stolen Payment out of it, which a freeze with no exceptions would block. A Customer Account still never goes negative.

## Consequences

- Only `OPS` can create a Reversal, so the bypass is limited to a trusted human role and every use is in the audit log.
- A Reversal is allowed once per Completed Transfer, in full; partial recovery of already-spent money is out of scope for v1.
