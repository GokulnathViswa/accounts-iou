package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByName
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.corda.lib.ci.RequestKeyForAccount
import com.template.states.AccountsIOUState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import java.util.*

/*
    Flow to get the AccountsIOUStates of the account passed
 */
@StartableByRPC
class GetIOUFlow(val accountID: UUID) :
        FlowLogic<List<StateAndRef<AccountsIOUState>>>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): List<StateAndRef<AccountsIOUState>> {
        // Return the AccountsIOUStates of the account
        var requestLists = serviceHub.vaultService.queryBy<AccountsIOUState>(QueryCriteria.VaultQueryCriteria(externalIds = listOf(accountID))).states
        return requestLists
    }
}
