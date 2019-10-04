package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.accountNameCriteria
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByName
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByUUID
import com.r3.corda.lib.accounts.workflows.flows.AllAccounts
import com.r3.corda.lib.accounts.workflows.flows.OurAccounts
import com.r3.corda.lib.ci.RequestKeyForAccount
//import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.template.contracts.AccountsIOUContract
import com.template.states.AccountsIOUState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import java.util.*

/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the IOU encapsulated
 * within an [IOUState].
 *
 * In our simple example, the [Acceptor] always accepts a valid IOU.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the IOU encapsulated
 * within an [IOUState].
 *
 * In our simple example, the [Acceptor] always accepts a valid IOU.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
object IOUFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(val iouValue: Int,
                    val lenderAccountName: String,
                    val borrowerAccountName:String) : FlowLogic<SignedTransaction>() {
        /*
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
        */
        companion object {
            object GENERATING_TRANSACTION : Step("Generating transaction based on new IOU.")
            object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
            object GATHERING_SIGS : Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        /**
         * The flow logic is encapsulated within the call() method.
         *  IOUFlow to create IOUState between accounts in different hosts
         */
        @Suspendable
        override fun call(): SignedTransaction {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION

            // Get the accountInfo of lender
            val lenderAccount = subFlow(AccountInfoByName(lenderAccountName)).single()
            // Get the host of lender account
            val lenderHost = lenderAccount.state.data.host
            // Get the UUID of lender account
            val lenderUUID = lenderAccount.state.data.identifier.id
            // Get the Anonymous Key of lender
            val lenderAccountKey = subFlow(RequestKeyForAccount(lenderHost, lenderUUID))

            // Get the accountInfo of borrower
            val borrowerAccount = subFlow(AccountInfoByName(borrowerAccountName)).single()
            // Get the host of borrower account
            val borrowerHost = borrowerAccount.state.data.host
            // Get the UUID of borrower account
            val borrowerUUID = borrowerAccount.state.data.identifier.id
            // Get the Anonymous Key of borrower
            val borrowerAccountKey = subFlow(RequestKeyForAccount(borrowerHost, borrowerUUID))

            // Set the WellKnownParty of borrower as counter party
            var counterparty: Party = borrowerAccount.state.data.host
            val wellKnownCounterParty = serviceHub.identityService.wellKnownPartyFromAnonymous(borrowerAccountKey)
            if(wellKnownCounterParty!= null) {
                counterparty = wellKnownCounterParty
            }
            // Create IOU state by passing the parameters to state
            val iouState = AccountsIOUState(iouValue, lenderUUID, lenderAccountKey, borrowerAccountKey, "IOU_CREATED", lenderUUID)

            // Make transaction to create IOUState by passing the CREATE command in contract and owning keys of lender and borrowers
            val txCommand = Command(AccountsIOUContract.Commands.CREATE(), listOf(lenderAccount.state.data.host.owningKey, borrowerAccountKey.owningKey))
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(iouState, AccountsIOUContract.ID)
                    .addCommand(txCommand)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder, listOfNotNull(serviceHub.myInfo.legalIdentities.first().owningKey, lenderAccountKey.owningKey))

            // Stage 4.
            progressTracker.currentStep = IOUFlow.Initiator.Companion.GATHERING_SIGS
            // Send the state to the counterparty, and receive it back with their signature.
            val otherPartySession = initiateFlow(counterparty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartySession), IOUFlow.Initiator.Companion.GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx,  setOf(otherPartySession), FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an IOU transaction." using (output is AccountsIOUState)
                    val iou = output as AccountsIOUState
                }
            }
            val txId = subFlow(signTransactionFlow).id
            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
        }
    }
}
