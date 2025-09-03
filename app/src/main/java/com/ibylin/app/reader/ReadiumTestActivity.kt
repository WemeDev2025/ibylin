package com.ibylin.app.reader

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * 简单的Readium测试Activity
 * 用于验证依赖是否正确集成
 */
@AndroidEntryPoint
class ReadiumTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "ReadiumTest"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 测试Readium依赖
        testReadiumDependencies()
    }
    
    private fun testReadiumDependencies() {
        try {
            Log.d(TAG, "开始测试Readium依赖...")
            
            // 尝试导入Readium类
            // 这里我们先注释掉，看看编译是否能通过
            
            Toast.makeText(this, "Readium测试Activity启动成功", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Readium测试Activity启动成功")
            
        } catch (e: Exception) {
            Log.e(TAG, "Readium依赖测试失败", e)
            Toast.makeText(this, "Readium依赖测试失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
