package com.squidink.alloy.core.data.datasource

/**
 * Interface for remote data sources (e.g., REST API, WebSocket, Firebase).
 *
 * Provides CRUD operations for remote data without observables.
 * Remote sources are typically used for synchronization and initial data fetch.
 *
 * @param T The type of data being managed
 * @param ID The type of the unique identifier
 */
interface RemoteDataSource<T, ID> {
    /**
     * Fetch a single item by its unique identifier from the remote source.
     *
     * @param id The unique identifier
     * @return The item if found, null otherwise
     */
    suspend fun fetchById(id: ID): T?

    /**
     * Fetch all items from the remote source.
     *
     * @return List of all items
     */
    suspend fun fetchAll(): List<T>

    /**
     * Create a new item in the remote source.
     *
     * @param item The item to create
     * @return The generated unique identifier for the created item
     */
    suspend fun create(item: T): ID

    /**
     * Update an existing item in the remote source.
     *
     * @param item The item to update
     */
    suspend fun update(item: T)

    /**
     * Delete an item by its unique identifier from the remote source.
     *
     * @param id The unique identifier of the item to delete
     */
    suspend fun delete(id: ID)
}
