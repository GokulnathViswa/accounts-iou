package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
/**
*This flow creates an account ([AccountInfo] State) of specified name  by calling [CreateAccount] flow in accounts library
* @param accountName is the proposed name for new account
 **/

@StartableByRPC
class CreateBankAccount(val accountName: String) :
    FlowLogic<StateAndRef<AccountInfo>>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): StateAndRef<AccountInfo> {
        //calling CreateAccount flow
        return subFlow(CreateAccount(accountName))
    }
}
