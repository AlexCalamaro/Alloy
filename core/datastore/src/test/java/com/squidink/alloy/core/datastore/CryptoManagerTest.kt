package com.squidink.alloy.core.datastore

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class CryptoManagerTest {

    @Test
    fun `encrypted data container holds values correctly`() {
        val ciphertext = "cipher".toByteArray()
        val iv = "iv123".toByteArray()
        val data1 = EncryptedData(ciphertext, iv)
        val data2 = EncryptedData(ciphertext.clone(), iv.clone())

        assertEquals(data1, data2)
        assertArrayEquals(data1.ciphertext, data2.ciphertext)
        assertArrayEquals(data1.iv, data2.iv)
    }
}
