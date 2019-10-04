package com.template.states

import com.template.contracts.AccountsIOUContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import java.security.PublicKey
import java.util.*

/**
 *@param value the value of the AccountIOU.
* @param lender the Annonymous party issuing the AccountsIOU.
* @param borrower the Annonymous party receiving the AccountsIOU
* @param id the UUID of the lenderAccount
*/
@BelongsToContract(AccountsIOUContract::class)
data class AccountsIOUState(val value: Int,
                            val lenderAccountID: UUID,
                            val lender: AnonymousParty,
                            val borrower: AnonymousParty,
                            val status: String,
                            val id: UUID,
                            val linearId: UniqueIdentifier = UniqueIdentifier()) : ContractState {

    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty>
    get() = listOf(lender, borrower)

    /**
     *The function to update @param [value]
     */
    fun updateValue(status: String) = copy(status = status)
}
