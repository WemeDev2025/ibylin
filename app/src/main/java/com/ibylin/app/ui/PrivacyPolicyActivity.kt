package com.ibylin.app.ui

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.ibylin.app.R

/**
 * 隐私政策页面
 */
class PrivacyPolicyActivity : AppCompatActivity() {
    
    private lateinit var btnBack: ImageButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)
        
        initViews()
        setupClickListeners()
    }
    
    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
    }
    
    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }
}
