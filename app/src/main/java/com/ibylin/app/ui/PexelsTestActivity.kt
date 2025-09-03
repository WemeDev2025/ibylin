package com.ibylin.app.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ibylin.app.R
import com.ibylin.app.utils.PexelsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PexelsTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "PexelsTestActivity"
    }
    
    private lateinit var btnTestSearch: Button
    private lateinit var btnTestCurated: Button
    private lateinit var tvResult: TextView
    
    private val pexelsManager = PexelsManager.getInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_pexels_test) // 暂时注释掉，布局文件缺失
        
        // 暂时显示一个简单的提示
        Toast.makeText(this, "Pexels测试功能暂未实现", Toast.LENGTH_LONG).show()
        finish()
        
        // initViews()
        // setupListeners()
    }
    
    // private fun initViews() {
    //     btnTestSearch = findViewById(R.id.btn_test_search)
    //     btnTestCurated = findViewById(R.id.btn_test_curated)
    //     tvResult = findViewById(R.id.tv_result)
    //     
    //     title = "Pexels API 测试"
    // }
    // 
    // private fun setupListeners() {
    //     btnTestSearch.setOnClickListener {
    //         testSearchAPI()
    //     }
    //     
    //     btnTestCurated.setOnClickListener {
    //         testSearchAPI()
    //     }
    // }
    
    private fun testSearchAPI() {
        tvResult.text = "正在测试搜索API..."
        btnTestSearch.isEnabled = false
        
        coroutineScope.launch {
            try {
                val photos = pexelsManager.searchCoverImages(
                    bookTitle = "三体",
                    author = "刘慈欣"
                )
                
                if (photos.isNotEmpty()) {
                    val result = "搜索成功！找到 ${photos.size} 张图片\n" +
                            "第一张图片信息：\n" +
                            "ID: ${photos[0].id}\n" +
                            "摄影师: ${photos[0].photographer}\n" +
                            "尺寸: ${photos[0].width}x${photos[0].height}\n" +
                            "URL: ${photos[0].src.medium}"
                    tvResult.text = result
                    Log.d(TAG, "搜索测试成功: ${photos.size} 张图片")
                } else {
                    tvResult.text = "搜索成功，但未找到图片"
                    Log.d(TAG, "搜索测试成功，但未找到图片")
                }
            } catch (e: Exception) {
                val errorMsg = "搜索测试失败: ${e.message}"
                tvResult.text = errorMsg
                Log.e(TAG, errorMsg, e)
            } finally {
                btnTestSearch.isEnabled = true
            }
        }
    }
    
    private fun testCuratedAPI() {
        tvResult.text = "正在测试精选API..."
        btnTestCurated.isEnabled = false
        
        coroutineScope.launch {
            try {
                val photos = pexelsManager.getCuratedPhotos(page = 1)
                
                if (photos.isNotEmpty()) {
                    val result = "精选API测试成功！找到 ${photos.size} 张图片\n" +
                            "第一张图片信息：\n" +
                            "ID: ${photos[0].id}\n" +
                            "摄影师: ${photos[0].photographer}\n" +
                            "尺寸: ${photos[0].width}x${photos[0].height}\n" +
                            "URL: ${photos[0].src.medium}"
                    tvResult.text = result
                    Log.d(TAG, "精选API测试成功: ${photos.size} 张图片")
                } else {
                    tvResult.text = "精选API测试成功，但未找到图片"
                    Log.d(TAG, "精选API测试成功，但未找到图片")
                }
            } catch (e: Exception) {
                val errorMsg = "精选API测试失败: ${e.message}"
                tvResult.text = errorMsg
                Log.e(TAG, errorMsg, e)
            } finally {
                btnTestCurated.isEnabled = true
            }
        }
    }
}
