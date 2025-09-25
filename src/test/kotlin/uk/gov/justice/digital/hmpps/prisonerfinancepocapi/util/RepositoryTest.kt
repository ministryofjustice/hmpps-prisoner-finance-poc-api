package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.util

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

// run JPA tests against the real database to avoid missing bugs arising from SQL syntax
@DataJpaTest
annotation class RepositoryTest
