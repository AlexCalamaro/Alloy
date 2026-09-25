package com.squidink.alloy.core.data.repository

import com.squidink.alloy.core.data.cache.CachePolicy
import com.squidink.alloy.core.data.cache.shouldUseCache
import com.squidink.alloy.core.data.cache.shouldUseRemote
import com.squidink.alloy.core.data.datasource.LocalDataSource
import com.squidink.alloy.core.data.datasource.RemoteDataSource
import com.squidink.alloy.core.data.mapper.DataMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Base repository providing common data layer operations.
 *
 * Implements repository pattern with support for:
 * - Local and remote data sources
 * - Cache policies for data retrieval strategies
 * - Domain-entity mapping via mappers
 * - Flow-based reactive streams
 *
 * @param Domain The domain model type exposed to the use cases/UI
 * @param Data The data model type from local/remote sources
 * @param ID The type of the unique identifier
 */
abstract class BaseRepository<Domain, Data, ID>(
    protected val localDataSource: LocalDataSource<Data, ID>,
    protected val remoteDataSource: RemoteDataSource<Data, ID>? = null
) {
    /**
     * Observe all items as a Flow of domain models.
     *
     * @return Flow emitting domain model list when data changes
     */
    fun observeAll(): Flow<List<Domain>> {
        return localDataSource.observeAll().map { dataList ->
            dataList.map { data -> mapToDomain(data) }
        }
    }

    /**
     * Observe a single item by ID as a Flow of domain model.
     *
     * @param id The unique identifier
     * @return Flow emitting the domain model when it changes
     */
    fun observeById(id: ID): Flow<Domain?> {
        return localDataSource.observeById(id).map { data ->
            data?.let { mapToDomain(it) }
        }
    }

    /**
     * Get all items from local storage.
     *
     * @return List of domain models
     */
    suspend fun getAll(): List<Domain> {
        return localDataSource.getAll().map { mapToDomain(it) }
    }

    /**
     * Get a single item by ID from local storage.
     *
     * @param id The unique identifier
     * @return The domain model or null if not found
     */
    suspend fun getById(id: ID): Domain? {
        return localDataSource.getById(id)?.let { mapToDomain(it) }
    }

    /**
     * Insert a new domain model.
     *
     * @param domain The domain model to insert
     * @return The generated unique identifier
     */
    suspend fun insert(domain: Domain): ID {
        val data = mapToData(domain)
        return localDataSource.insert(data)
    }

    /**
     * Update an existing domain model.
     *
     * @param domain The domain model to update
     */
    suspend fun update(domain: Domain) {
        val data = mapToData(domain)
        localDataSource.update(data)
    }

    /**
     * Delete an item by ID.
     *
     * @param id The unique identifier of the item to delete
     */
    suspend fun delete(id: ID) {
        localDataSource.delete(id)
    }

    /**
     * Refresh data from remote source and update local cache.
     *
     * Fetches all data from remote, maps to data models, and stores locally.
     * Throws exception if remote source is not available.
     */
    suspend fun refreshFromRemote() {
        val remote = remoteDataSource ?: throw IllegalStateException(
            "RemoteDataSource not configured for this repository"
        )
        
        val remoteData = remote.fetchAll()
        // Clear local and replace with fresh data
        localDataSource.getAll().forEach { localDataSource.delete(getId(it)) }
        remoteData.forEach { localDataSource.insert(it) }
    }

    /**
     * Refresh a single item from remote source.
     *
     * @param id The unique identifier of the item to refresh
     */
    suspend fun refreshById(id: ID) {
        val remote = remoteDataSource ?: throw IllegalStateException(
            "RemoteDataSource not configured for this repository"
        )
        
        val remoteData = remote.fetchById(id)
        remoteData?.let { localDataSource.update(it) }
    }

    /**
     * Fetch data using a specific cache policy.
     *
     * @param policy The cache policy to use
     * @return Flow emitting the domain model
     */
    suspend fun fetchWithPolicy(policy: CachePolicy): Flow<Domain?> {
        return when {
            policy.shouldUseRemote() && remoteDataSource != null -> {
                // Try remote first
                val remoteData = remoteDataSource.fetchById(getIdFromDomain())
                if (remoteData != null) {
                    localDataSource.update(remoteData)
                    flow { emit(mapToDomain(remoteData)) }
                } else if (policy.shouldUseCache()) {
                    // Fall back to cache
                    observeById(getIdFromDomain())
                } else {
                    flow { }
                }
            }
            policy.shouldUseCache() -> {
                observeById(getIdFromDomain())
            }
            else -> {
                flow { }
            }
        }
    }

    /**
     * Get the ID from a data model for deletion/update operations.
     * Subclasses should override this for their specific ID type.
     *
     * @param data The data model
     * @return The unique identifier
     */
    protected abstract fun getId(data: Data): ID

    /**
     * Get the ID from a domain model.
     * Subclasses should override this for their specific ID type.
     *
     * @return The unique identifier
     */
    protected abstract fun getIdFromDomain(): ID

    /**
     * Map a data model to a domain model.
     *
     * @param data The data model
     * @return The domain model
     */
    protected abstract fun mapToDomain(data: Data): Domain

    /**
     * Map a domain model to a data model.
     *
     * @param domain The domain model
     * @return The data model
     */
    protected abstract fun mapToData(domain: Domain): Data
}

/**
 * Base repository with mapper support for cleaner mapping logic.
 *
 * @param Domain The domain model type
 * @param Data The data model type
 * @param ID The unique identifier type
 */
abstract class MappedRepository<Domain, Data, ID>(
    localDataSource: LocalDataSource<Data, ID>,
    remoteDataSource: RemoteDataSource<Data, ID>? = null,
    private val toDomainMapper: DataMapper<Data, Domain>,
    private val toDataMapper: DataMapper<Domain, Data>
) : BaseRepository<Domain, Data, ID>(localDataSource, remoteDataSource) {
    
    override fun mapToDomain(data: Data): Domain = toDomainMapper.map(data)
    override fun mapToData(domain: Domain): Data = toDataMapper.map(domain)
}
