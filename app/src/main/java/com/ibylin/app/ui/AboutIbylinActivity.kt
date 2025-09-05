package com.ibylin.app.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ibylin.app.databinding.ActivityAboutIbylinBinding

/**
 * 关于iBylin页面Activity
 */
class AboutIbylinActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "AboutIbylinActivity"
    }
    
    private lateinit var binding: ActivityAboutIbylinBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutIbylinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        Log.d(TAG, "AboutIbylinActivity onCreate")
        
        setupViews()
    }
    
    private fun setupViews() {
        // 设置标题
        supportActionBar?.title = "关于iBylin"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
