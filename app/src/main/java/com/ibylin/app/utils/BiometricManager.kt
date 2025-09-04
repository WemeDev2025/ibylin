package com.ibylin.app.utils

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {
    
    private const val TAG = "BiometricHelper"
    
    /**
     * 检查设备是否支持生物识别
     */
    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = androidx.biometric.BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS -> true
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.w(TAG, "设备不支持生物识别")
                false
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.w(TAG, "生物识别硬件不可用")
                false
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.w(TAG, "未注册生物识别")
                false
            }
            else -> {
                Log.w(TAG, "生物识别不可用")
                false
            }
        }
    }
    
    /**
     * 显示指纹识别对话框
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        
        val biometricPrompt = androidx.biometric.BiometricPrompt(activity, executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, "生物识别错误: $errorCode - $errString")
                    when (errorCode) {
                        1 -> onError("设备不支持指纹识别")
                        2 -> onError("指纹识别硬件不可用")
                        3 -> onError("未注册指纹")
                        4 -> onError("指纹识别被锁定，请稍后重试")
                        5 -> onError("指纹识别被永久锁定")
                        6 -> onError("操作被取消")
                        else -> onError("指纹识别失败: $errString")
                    }
                }
                
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d(TAG, "生物识别成功")
                    onSuccess()
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "生物识别失败")
                    onFailed()
                }
            })
        
        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("取消")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    /**
     * 检查生物识别类型
     */
    fun getBiometricType(context: Context): String {
        val biometricManager = androidx.biometric.BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS -> "强生物识别"
            else -> when (biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS -> "弱生物识别"
                else -> "不支持"
            }
        }
    }
}
