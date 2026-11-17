package com.game6789y.maahatsuboshi

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest

/**
 * Creates a device-local RSA identity in Android Keystore.
 *
 * This is a secure-storage foundation only. It is not yet encoded as an ADB
 * public key and is not used by an ADB pairing or authentication transport.
 */
object LocalAdbIdentity {
    private const val PROVIDER = "AndroidKeyStore"
    private const val ALIAS = "maahatsuboshi_adb_identity_v1"

    fun loadOrCreate(): String {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        if (!keyStore.containsAlias(ALIAS)) {
            val generator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                PROVIDER
            )
            generator.initialize(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                    .setKeySize(2048)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .setUserAuthenticationRequired(false)
                    .build()
            )
            generator.generateKeyPair()
        }

        val certificate = keyStore.getCertificate(ALIAS)
            ?: error("Android Keystore identity certificate is unavailable")
        val fingerprint = MessageDigest.getInstance("SHA-256")
            .digest(certificate.publicKey.encoded)
            .joinToString(":") { "%02X".format(it) }
        return fingerprint
    }

    fun exists(): Boolean =
        KeyStore.getInstance(PROVIDER).run {
            load(null)
            containsAlias(ALIAS)
        }
}
