package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.accountNameCriteria
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByName
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByUUID
import com.r3.corda.lib.accounts.workflows.flows.RequestAccountInfo
import com.r3.corda.lib.ci.RequestKeyForAccount
//import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.template.contracts.AccountsIOUContract
import com.template.states.AccountsIOUState
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
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
object AcceptIOUFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(val borrowerAccountName: String,
                    val lenderAccountID: String,
                    val lenderParty: String
    ) : FlowLogic<SignedTransaction>() {
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
         */
        @Suspendable
        override fun call(): SignedTransaction {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

// Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
// Generate an unsigned transaction.

            val hostParty= serviceHub.identityService.partiesFromName(lenderParty, false).single()
            val lenderAccountUUID = UUID.fromString(lenderAccountID)
            val account = subFlow(RequestAccountInfo(lenderAccountUUID, hostParty))
            val lenderHost = account!!.host
            val lenderUUID = account!!.identifier.id
            //  val lenderAccountKey = subFlow(RequestKeyForAccount(lenderHost, lenderUUID))
//        val counterParty =  serviceHub.identityService.wellKnownPartyFromAnonymous(lenderAccountKey)
//        println("counterParty3   "+counterParty)

            val tx = subFlow(AccountInfoByName(borrowerAccountName))
            val borrowerAccount = tx.single()
            val borrowerHost = borrowerAccount.state.data.host
            val borrowerUUID = borrowerAccount.state.data.identifier.id
            //  val borrowerAccountKey = subFlow(RequestKeyForAccount(borrowerHost, borrowerUUID))

            val borrowerIOUState = serviceHub.vaultService.queryBy<AccountsIOUState>(QueryCriteria.VaultQueryCriteria(externalIds = listOf(borrowerUUID))).states.filter { it.state.data.status == "IOU_CREATED" }.single()
            println("borrowerIOUState"+borrowerIOUState)

            val newState = borrowerIOUState.state.data.updateValue("IOU_ACCEPTED")
            println("newState"+newState)

            val txCommand = Command(AccountsIOUContract.Commands.UPDATE(), listOf(borrowerIOUState.state.data.lender.owningKey, serviceHub.myInfo.legalIdentities.first().owningKey))
            val txBuilder= TransactionBuilder(notary)
                .addInputState(borrowerIOUState)
                .addOutputState(newState, AccountsIOUContract.ID)
                .addCommand(txCommand)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder, listOfNotNull(serviceHub.myInfo.legalIdentities.first().owningKey, borrowerIOUState.state.data.borrower.owningKey))

            // Stage 4.
            progressTracker.currentStep = GATHERING_SIGS
            // Send the state to the counterparty, and receive it back with their signature.
            val otherPartySession = initiateFlow(hostParty)
            val fullySignedTx = subFlow(CollectSignatureFlow(partSignedTx, otherPartySession, borrowerIOUState.state.data.lender.owningKey))
            val signedByCounterParty = partSignedTx.withAdditionalSignatures(fullySignedTx)
            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(signedByCounterParty, listOf(otherPartySession).filter { it.counterparty != ourIdentity }))
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    val iou = output as AccountsIOUState
                    "This must be an IOU accept transaction." using(iou.status == "IOU_ACCEPTED")
                }
            }
            val txId = subFlow(signTransactionFlow).id
            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
        }
    }
}


