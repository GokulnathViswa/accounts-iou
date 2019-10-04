package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByName
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

/**
*This flow shares the given account in a Party with another Party using the accounts library function [ShareAccountInfo]
* @param accountName-name of the account to be shared
* @param toHost-name of the Party with which the account is to be shared
 * */

@StartableByRPC
class ShareAccountInfoFlow(val accountName: String, val toHost: String) :
        FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()
    @Suspendable
    override fun call() {

        /**Getting the [AccountInfo] state of the account from account name using [AccountInfoByName] flow in accounts library*/
        val tx = subFlow(AccountInfoByName(accountName))
        val account = tx.single()

        //Getting the Party from partyName using identityService
        val toHostParty= serviceHub.identityService.partiesFromName(toHost, false).single()

        return subFlow(ShareAccountInfo(account, listOf(toHostParty)))
    }
}
