package com.yangchengwei.easytrip.assistant

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*

/** The file is excluded from Android backup; ciphertext cannot be read without this device's key. */
class AssistantConfigStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "assistant-provider.enc"))
    private val changes = MutableStateFlow(0L)
    val revision = changes.asStateFlow()
    private val alias = "easy-trip-assistant-provider-v1"

    private fun readJson(): JsonObject? = runCatching {
        if (!file.baseFile.exists()) return null
        val data = ByteBuffer.wrap(file.readFully())
        val ivLength = data.get().toInt()
        require(ivLength == 12)
        val iv = ByteArray(ivLength).also(data::get)
        val payload = ByteArray(data.remaining()).also(data::get)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        Json.parseToJsonElement(cipher.doFinal(payload).toString(Charsets.UTF_8)).jsonObject
    }.getOrNull()
    @Synchronized fun read(): ProviderConfig? = runCatching {
        val json = readJson() ?: return null
        ProviderConfig(json.getValue("url").jsonPrimitive.content, json.getValue("model").jsonPrimitive.content,
            json.getValue("key").jsonPrimitive.content).validated()
    }.getOrNull()

    @Synchronized fun save(config: ProviderConfig) {
        val value = config.validated()
        if (value.sameAs(read())) return
        val json = buildJsonObject { put("url", value.baseUrl); put("model", value.model); put("key", value.apiKey) }
        write(json)
        changes.value++
    }
    @Synchronized fun imageCapability(): ImageCapabilityResult {
        val json = readJson()
        val value = runCatching { ImageCapability.valueOf(json?.get("imageCapability")?.jsonPrimitive?.content.orEmpty()) }
            .getOrDefault(ImageCapability.UNKNOWN)
        return ImageCapabilityResult(value, json?.get("imageCheckedAt")?.jsonPrimitive?.longOrNull)
    }
    @Synchronized fun saveImageCapability(expected: ProviderConfig, result: ImageCapabilityResult): Boolean {
        if (!expected.sameAs(read())) return false
        val json = readJson()?.toMutableMap() ?: return false
        json["imageCapability"] = JsonPrimitive(result.capability.name)
        result.checkedAt?.let { json["imageCheckedAt"] = JsonPrimitive(it) }
        write(JsonObject(json))
        return true
    }
    private fun write(json: JsonObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
        val stream = file.startWrite()
        try {
            stream.write(byteArrayOf(cipher.iv.size.toByte())); stream.write(cipher.iv); stream.write(encrypted)
            file.finishWrite(stream)
        } catch (e: Exception) { file.failWrite(stream); throw AssistantFailure("无法安全保存配置，原配置未替换。") }
    }

    @Synchronized fun clear() { file.delete(); changes.value++ }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
}
