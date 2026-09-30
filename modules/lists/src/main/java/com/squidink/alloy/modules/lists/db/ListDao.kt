package com.squidink.alloy.modules.lists.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {

    @Transaction
    @Query("SELECT * FROM keep_lists ORDER BY isPinned DESC, updatedAt DESC")
    fun getListsWithItems(): Flow<List<ListWithItemsRelation>>

    @Transaction
    @Query("SELECT * FROM keep_lists WHERE id = :id")
    fun getListWithItemsById(id: String): Flow<ListWithItemsRelation?>

    @Upsert
    suspend fun upsertList(list: ListEntity)

    @Upsert
    suspend fun upsertItems(items: List<ListItemEntity>)

    @Query("DELETE FROM keep_list_items WHERE listId = :listId")
    suspend fun deleteItemsForList(listId: String)

    @Transaction
    suspend fun saveListWithItems(list: ListEntity, items: List<ListItemEntity>) {
        upsertList(list)
        deleteItemsForList(list.id)
        if (items.isNotEmpty()) {
            upsertItems(items)
        }
    }

    @Query("DELETE FROM keep_lists WHERE id = :id")
    suspend fun deleteList(id: String)

    @Query("UPDATE keep_list_items SET isCompleted = :isCompleted WHERE id = :itemId")
    suspend fun updateItemCompletion(itemId: String, isCompleted: Boolean)

    @Query("UPDATE keep_lists SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :listId")
    suspend fun updatePinState(listId: String, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE keep_lists SET colorHex = :colorHex, updatedAt = :updatedAt WHERE id = :listId")
    suspend fun updateColor(listId: String, colorHex: Long, updatedAt: Long = System.currentTimeMillis())
}
