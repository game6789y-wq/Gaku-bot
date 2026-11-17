package com.game6789y.maahatsuboshi

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.system.Os
import com.flyfishxu.kadb.cert.KadbCert
import com.flyfishxu.kadb.cert.KadbPrivateKeyStore
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Persists Kadb's RSA host identity encrypted at rest.
 *
 * The AES-256-GCM wrapping key is held by Android Keystore. The RSA private
 * key is decrypted only when Kadb needs it for ADB protocol operations.
 */
object LocalAdbIdentity {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val WRAPPING_KEY_ALIAS = "maahatsuboshi_adb_wrapping_key_v1"
    private const val FILE_NAME = "adb-host-key.enc"
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128

    @Volatile
    private var configured = false

    @Synchronized
    fun loadOrCreate(context: Context): String {
        if (!configured) {
            KadbCert.configure(store = EncryptedAndroidKeyStore(context.applicationContext))
            configured = true
        }
        return KadbCert.ensureReady().fingerprintSha256
    }

    private class EncryptedAndroidKeyStore(context: Context) : KadbPrivateKeyStore {
        private val file = File(context.noBackupFilesDir, FILE_NAME)
        private val lock = Any()

        override fun readPrivateKeyPem(): ByteArray? = synchronized(lock) {
            if (!file.exists()) return@synchronized null
            val encrypted = file.readBytes()
            require(encrypted.size > IV_LENGTH + 16) { "Encrypted ADB identity is truncated" }
            val iv = encrypted.copyOfRange(0, IV_LENGTH)
            val ciphertext = encrypted.copyOfRange(IV_LENGTH, encrypted.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            cipher.doFinal(ciphertext)
        }

        override fun writePrivateKeyPemAtomic(privateKeyPem: ByteArray) = synchronized(lock) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(privateKeyPem)
            val payload = iv + ciphertext

            val temp = File(file.parentFile, "$FILE_NAME.tmp")
            FileOutputStream(temp).use { stream ->
                stream.write(payload)
                stream.fd.sync()
            }
            Os.rename(temp.absolutePath, file.absolutePath)
        }

        override fun clear() = synchronized(lock) {
            if (file.exists() && !file.delete()) {
                throw IllegalStateException("Could not delete encrypted ADB identity")
            }
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (keyStore.containsAlias(WRAPPING_KEY_ALIAS)) {
                keyStore.deleteEntry(WRAPPING_KEY_ALIAS)
            }
        }

        private fun wrappingKey(): SecretKey {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            (keyStore.getKey(WRAPPING_KEY_ALIAS, null) as? SecretKey)?.let { return it }

            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            generator.init(
                KeyGenParameterSpec.Builder(
                    WRAPPING_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .setUserAuthenticationRequired(false)
                    .build()
            )
            return generator.generateKey()
        }
    }
}
