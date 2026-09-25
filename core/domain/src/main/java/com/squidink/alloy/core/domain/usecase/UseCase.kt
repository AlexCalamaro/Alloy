package com.squidink.alloy.core.domain.usecase

/**
 * Base interface for all Use Cases in the domain layer.
 *
 * Use Cases encapsulate business logic and are independent of:
 * - UI framework (Compose, Views)
 * - Data sources (Room, DataStore, Network)
 * - Dependency injection framework
 *
 * This allows for:
 * - Easy unit testing
 * - Reuse across different modules
 * - Clear separation of concerns
 *
 * @param Input The input type for the use case
 * @param Output The output type returned by the use case
 */
interface UseCase<Input, Output> {
    /**
     * Executes the business logic with the given input.
     *
     * @param input The input data for the use case
     * @return The result of executing the use case
     */
    suspend fun execute(input: Input): Output
}

/**
 * A Use Case that doesn't require input parameters.
 *
 * @param Output The output type returned by the use case
 */
interface SimpleUseCase<Output> : UseCase<Unit, Output> {
    override suspend fun execute(input: Unit): Output
}

/**
 * A Use Case that performs an action and returns Unit.
 *
 * @param Input The input type for the use case
 */
interface ActionUseCase<Input> : UseCase<Input, Unit>
