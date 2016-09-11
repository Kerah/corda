package com.r3corda.contracts.universal

import com.r3corda.core.contracts.BusinessCalendar
import com.r3corda.core.contracts.Frequency
import com.r3corda.core.utilities.DUMMY_NOTARY
import com.r3corda.testing.transaction
import org.junit.Test
import java.time.Instant

/**
 * Created by sofusmortensen on 08/09/16.
 */

class RollOutTests {

    val TEST_TX_TIME_1: Instant get() = Instant.parse("2017-09-02T12:00:00.00Z")

    val contract = arrange {
        rollOut("2016-09-01".ld, "2017-09-01".ld, Frequency.Monthly) {
            (acmeCorp or highStreetBank).may {
                "transfer".givenThat(after(end)) {
                    highStreetBank.gives(acmeCorp, 10.K, USD)
                    next()
                }
            }
        }
    }
    val stateStart = UniversalContract.State(listOf(DUMMY_NOTARY.owningKey), contract)

    val contractStep1a = arrange {
        rollOut("2016-12-01".ld, "2017-09-01".ld, Frequency.Monthly) {
            (acmeCorp or highStreetBank).may {
                "transfer".givenThat(after(end)) {
                    highStreetBank.gives(acmeCorp, 10.K, USD)
                    next()
                }
            }
        }
    }

    val contractStep1b = arrange {
        highStreetBank.gives(acmeCorp, 10.K, USD)
    }

    val stateStep1a = UniversalContract.State(listOf(DUMMY_NOTARY.owningKey), contractStep1a)
    val stateStep1b = UniversalContract.State(listOf(DUMMY_NOTARY.owningKey), contractStep1b)

    @Test
    fun dateTests() {

        val d1 = BusinessCalendar.parseDateFromString("2016-09-10")
    }

    @Test
    fun issue() {
        transaction {
            output { stateStart }
            timestamp(TEST_TX_TIME_1)

            this `fails with` "transaction has a single command"

            tweak {
                command(acmeCorp.owningKey) { UniversalContract.Commands.Issue() }
                this `fails with` "the transaction is signed by all liable parties"
            }

            command(highStreetBank.owningKey) { UniversalContract.Commands.Issue() }

            this.verifies()
        }
    }

    @Test
    fun `execute`() {
        transaction {
            input { stateStart }
            output { stateStep1a }
            output { stateStep1b }
            timestamp(TEST_TX_TIME_1)

            tweak {
                command(highStreetBank.owningKey) { UniversalContract.Commands.Action("some undefined name") }
                this `fails with` "action must be defined"
            }

            command(highStreetBank.owningKey) { UniversalContract.Commands.Action("exercise") }

            this.verifies()
        }
    }

}