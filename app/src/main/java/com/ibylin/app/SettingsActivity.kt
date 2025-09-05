package com.ibylin.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.ibylin.app.databinding.ActivitySettingsBinding
import com.ibylin.app.ui.UserAgreementActivity
import com.ibylin.app.ui.PrivacyPolicyActivity
import com.ibylin.app.ui.AboutIbylinActivity

/**
 * 设置页面Activity
 */
class SettingsActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "SettingsActivity"
    }
    
    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        Log.d(TAG, "SettingsActivity onCreate")
        
        setupViews()
        setupClickListeners()
    }
    
    private fun setupViews() {
        // 设置标题
        supportActionBar?.title = "设置"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun setupClickListeners() {
        // 返回按钮
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // 用户协议
        binding.cvUserAgreement.setOnClickListener {
            openUserAgreement()
        }
        
        // 隐私政策
        binding.cvPrivacyPolicy.setOnClickListener {
            openPrivacyPolicy()
        }
        
        // 关于ibylin
        binding.cvAboutIbylin.setOnClickListener {
            openAboutIbylin()
        }
        
        // 商店评价
        binding.cvStoreRating.setOnClickListener {
            openStoreRating()
        }
        
        // 联系我们
        binding.cvContactUs.setOnClickListener {
            openContactUs()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    /**
     * 打开用户协议
     */
    private fun openUserAgreement() {
        try {
            Log.d(TAG, "打开用户协议")
            val intent = Intent(this, UserAgreementActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "打开用户协议失败", e)
            Toast.makeText(this, "打开用户协议失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 打开隐私政策
     */
    private fun openPrivacyPolicy() {
        try {
            Log.d(TAG, "打开隐私政策")
            val intent = Intent(this, PrivacyPolicyActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "打开隐私政策失败", e)
            Toast.makeText(this, "打开隐私政策失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 打开关于ibylin
     */
    private fun openAboutIbylin() {
        try {
            Log.d(TAG, "打开关于ibylin")
            val intent = Intent(this, AboutIbylinActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "打开关于ibylin失败", e)
            Toast.makeText(this, "打开关于ibylin失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 打开商店评价
     */
    private fun openStoreRating() {
        try {
            Toast.makeText(this, "商店评价", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "打开商店评价")
            // TODO: 实现商店评价跳转
        } catch (e: Exception) {
            Log.e(TAG, "打开商店评价失败", e)
            Toast.makeText(this, "打开商店评价失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 打开联系我们
     */
    private fun openContactUs() {
        try {
            Log.d(TAG, "复制微信到剪贴板")
            copyWechatToClipboard()
        } catch (e: Exception) {
            Log.e(TAG, "复制微信失败", e)
            Toast.makeText(this, "复制微信失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 复制微信到剪贴板
     */
    private fun copyWechatToClipboard() {
        try {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("微信", "task1834")
            clipboardManager.setPrimaryClip(clipData)
            
            Toast.makeText(this, "微信已复制", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "微信已复制到剪贴板: task1834")
        } catch (e: Exception) {
            Log.e(TAG, "复制微信到剪贴板失败", e)
            Toast.makeText(this, "复制失败", Toast.LENGTH_SHORT).show()
        }
    }
}
