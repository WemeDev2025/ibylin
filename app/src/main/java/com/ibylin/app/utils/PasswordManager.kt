package com.ibylin.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object PasswordManager {
    
    private const val TAG = "PasswordManager"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "BookLockPassword"
    private const val PREFS_NAME = "book_lock_password"
    private const val KEY_PASSWORD_HASH = "password_hash"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 设置密码
     */
    fun setPassword(context: Context, password: String): Boolean {
        return try {
            val hashedPassword = hashPassword(password)
            val prefs = getPrefs(context)
            prefs.edit().putString(KEY_PASSWORD_HASH, hashedPassword).apply()
            Log.d(TAG, "密码设置成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "设置密码失败", e)
            false
        }
    }
    
    /**
     * 验证密码
     */
    fun verifyPassword(context: Context, password: String): Boolean {
        return try {
            val storedHash = getPrefs(context).getString(KEY_PASSWORD_HASH, null)
            if (storedHash == null) {
                Log.w(TAG, "未设置密码")
                return false
            }
            
            val inputHash = hashPassword(password)
            val isValid = storedHash == inputHash
            Log.d(TAG, "密码验证: $isValid")
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "验证密码失败", e)
            false
        }
    }
    
    /**
     * 检查是否已设置密码
     */
    fun hasPassword(context: Context): Boolean {
        return getPrefs(context).getString(KEY_PASSWORD_HASH, null) != null
    }
    
    /**
     * 移除密码
     */
    fun removePassword(context: Context): Boolean {
        return try {
            val prefs = getPrefs(context)
            prefs.edit().remove(KEY_PASSWORD_HASH).apply()
            Log.d(TAG, "密码已移除")
            true
        } catch (e: Exception) {
            Log.e(TAG, "移除密码失败", e)
            false
        }
    }
    
    /**
     * 简单的密码哈希（实际应用中应使用更安全的方法）
     */
    private fun hashPassword(password: String): String {
        return try {
            val bytes = password.toByteArray()
            var hash = 0L
            for (byte in bytes) {
                hash = (hash * 31 + byte.toLong()) and 0xFFFFFFFFL
            }
            hash.toString()
        } catch (e: Exception) {
            Log.e(TAG, "密码哈希失败", e)
            password.hashCode().toString()
        }
    }
    
    /**
     * 生成加密密钥
     */
    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build()
        
        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }
    
    /**
     * 加密数据
     */
    fun encryptData(data: String): ByteArray? {
        return try {
            val key = generateKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            
            val encrypted = cipher.doFinal(data.toByteArray())
            val iv = cipher.iv
            
            // 组合IV和加密数据
            val result = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, result, 0, iv.size)
            System.arraycopy(encrypted, 0, result, iv.size, encrypted.size)
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "加密数据失败", e)
            null
        }
    }
    
    /**
     * 解密数据
     */
    fun decryptData(encryptedData: ByteArray): String? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            
            if (secretKey == null) {
                Log.e(TAG, "密钥不存在")
                return null
            }
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = encryptedData.copyOfRange(0, 12)
            val encrypted = encryptedData.copyOfRange(12, encryptedData.size)
            
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decrypted = cipher.doFinal(encrypted)
            String(decrypted)
        } catch (e: Exception) {
            Log.e(TAG, "解密数据失败", e)
            null
        }
    }
}
