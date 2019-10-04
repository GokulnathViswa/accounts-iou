package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [AccountsIOUState], which in turn encapsulates an [AccountsIOU] and [UpdateAccountsIOU].
 *
 * For a new [AccountsIOU] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [AccountsIOU].
 *
 *
 *  For  [UpdateAccountsIOU] to be issued onto the ledger, a transaction is required which takes:
 * - one input states :[AccountsIOU].
 * - One output state: [UpdateAccountsIOU].
 *
 *
 * All contracts must sub-class the [Contract] interface.
 */
class AccountsIOUContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.AccountsIOUContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.

    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val command = tx.commands.requireSingleCommand<Commands>()
        val setOfSigners = command.signers.toSet()
        when (command.value) {
            is Commands.CREATE -> create(tx, setOfSigners)
            is Commands.UPDATE -> update(tx, setOfSigners)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }
    private fun create(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        // Generic constraints around the AccountsIOU transaction for create function.

        "No inputs should be consumed when issuing an IOU." using (tx.inputs.isEmpty())
        "Only one output state should be created." using (tx.outputs.size == 1)

    }
    private fun update(tx: LedgerTransaction,signers: Set<PublicKey>) = requireThat {
        // Generic constraints around the AccountsIOU transaction for update function.
        "One inputs should be consumed when issuing an IOU." using (tx.inputs.size==1)
        "Only one output state should be created." using (tx.outputs.size == 1)
    }

    /**
     * This contract implements two commands, CREATE and UPDATE
     */
    interface Commands : CommandData {
        class CREATE : Commands
        class UPDATE : Commands
    }}