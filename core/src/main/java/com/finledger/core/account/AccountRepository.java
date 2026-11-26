package com.finledger.core.account;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data writes the implementation: save, findById, ... become SQL via Hibernate. */
public interface AccountRepository extends JpaRepository<Account, UUID> {
}
