package com.squidink.alloy.core.datastore

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared factory that generates or retrieves a secure random 32-byte passphrase,
 * encrypts it via Keystore (CryptoManager), stores it in DataStore,
 * and yields the SQLCipher SupportOpenHelperFactory.
 *
 * IMPORTANT: All methods are suspend functions to avoid blocking the calling thread.
 * Database factory creation should be called from a background dispatcher.
 */
@Singleton
class EncryptedRoomFactory @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val cryptoManager: CryptoManager
) {

    /**
     * Creates a SupportSQLiteOpenHelper.Factory for the specified database.
     *
     * This is a suspend function that performs I/O operations (DataStore read/write)
     * and cryptographic operations on a background dispatcher.
     *
     * @param dbName The name of the database (without .db extension)
     * @return SupportSQLiteOpenHelper.Factory configured with the encrypted passphrase
     */
    suspend fun getFactoryFor(dbName: String): SupportSQLiteOpenHelper.Factory {
        return withContext(Dispatchers.IO) {
            val encodedIv = dataStoreManager.getStringFlow("${dbName}_iv").first()
            val encodedCiphertext = dataStoreManager.getStringFlow("${dbName}_ciphertext").first()

            val passphraseBytes = if (encodedIv == null || encodedCiphertext == null) {
                generateAndSaveNewKey(dbName)
            } else {
                try {
                    val iv = Base64.decode(encodedIv, Base64.NO_WRAP)
                    val ciphertext = Base64.decode(encodedCiphertext, Base64.NO_WRAP)
                    cryptoManager.decrypt(EncryptedData(ciphertext, iv))
                } catch (e: Exception) {
                    generateAndSaveNewKey(dbName)
                }
            }

            SupportOpenHelperFactory(passphraseBytes)
        }
    }

    private suspend fun generateAndSaveNewKey(dbName: String): ByteArray {
        val rawKey = cryptoManager.generateSecureRandomBytes(32)
        val encrypted = cryptoManager.encrypt(rawKey)

        dataStoreManager.setString("${dbName}_iv", Base64.encodeToString(encrypted.iv, Base64.NO_WRAP))
        dataStoreManager.setString("${dbName}_ciphertext", Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP))

        return rawKey
    }

    companion object {
        init {
            try {
                System.loadLibrary("sqlcipher")
            } catch (_: UnsatisfiedLinkError) {
                // Ignored in unit test JVM environments without native libraries
            }
        }
    }
}
