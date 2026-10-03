package com.squidink.alloy.core.module

/**
 * Interface for LLM Host module actions.
 *
 * Defines actions and queries that can be triggered on the LLM Host module
 * from other parts of the app (e.g. Scratch, GitDesk, Scenes).
 */
interface ILlmHostActions : IModuleActions {

    /**
     * Start the localhost LLM host server.
     */
    fun startHost()

    /**
     * Stop the localhost LLM host server.
     */
    fun stopHost()

    /**
     * Check if the host server is currently running.
     */
    fun isHostRunning(): Boolean

    /**
     * Query the hosted model directly from in-app modules with a prompt.
     *
     * @param prompt The user or system prompt to process.
     * @return Generated text completion from the active model.
     */
    suspend fun query(prompt: String): String
}
