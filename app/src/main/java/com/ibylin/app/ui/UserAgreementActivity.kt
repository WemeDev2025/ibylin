package com.ibylin.app.ui

import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.ibylin.app.R

class UserAgreementActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var toolbar: Toolbar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_agreement)
        
        initViews()
        setupToolbar()
        loadUserAgreement()
    }
    
    private fun initViews() {
        webView = findViewById(R.id.webview_user_agreement)
        toolbar = findViewById(R.id.toolbar_user_agreement)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "用户协议"
    }
    
    private fun loadUserAgreement() {
        webView.settings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false
            }
        }
        
        // 加载用户协议HTML内容
        val htmlContent = generateUserAgreementHTML()
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }
    
    private fun generateUserAgreementHTML(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>用户协议</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 800px;
                    margin: 0 auto;
                    padding: 20px;
                    background-color: #f8f9fa;
                }
                .container {
                    background: white;
                    padding: 30px;
                    border-radius: 12px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                }
                h1 {
                    color: #1EB4A2;
                    text-align: center;
                    margin-bottom: 30px;
                    font-size: 28px;
                }
                h2 {
                    color: #2c3e50;
                    margin-top: 30px;
                    margin-bottom: 15px;
                    font-size: 20px;
                    border-bottom: 2px solid #1EB4A2;
                    padding-bottom: 5px;
                }
                h3 {
                    color: #34495e;
                    margin-top: 20px;
                    margin-bottom: 10px;
                    font-size: 16px;
                }
                p {
                    margin-bottom: 15px;
                    text-align: justify;
                }
                ul, ol {
                    margin-bottom: 15px;
                    padding-left: 20px;
                }
                li {
                    margin-bottom: 8px;
                }
                .highlight {
                    background-color: #e8f5e8;
                    padding: 15px;
                    border-left: 4px solid #1EB4A2;
                    margin: 20px 0;
                    border-radius: 4px;
                }
                .warning {
                    background-color: #fff3cd;
                    padding: 15px;
                    border-left: 4px solid #ffc107;
                    margin: 20px 0;
                    border-radius: 4px;
                }
                .footer {
                    text-align: center;
                    margin-top: 40px;
                    padding-top: 20px;
                    border-top: 1px solid #eee;
                    color: #666;
                    font-size: 14px;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>用户服务协议</h1>
                
                <div class="highlight">
                    <p><strong>欢迎使用iBylin电子书阅读器！</strong></p>
                    <p>本协议是您与iBylin电子书阅读器（以下简称"本应用"）之间关于您使用本应用服务所订立的协议。请您仔细阅读本协议，特别是免除或者限制责任的条款。</p>
                </div>
                
                <h2>1. 服务说明</h2>
                <p>iBylin是一款专业的电子书阅读应用，支持EPUB、PDF等多种格式的电子书阅读。我们致力于为用户提供优质的阅读体验。</p>
                
                <h3>1.1 主要功能</h3>
                <ul>
                    <li>支持EPUB、PDF等主流电子书格式</li>
                    <li>智能书籍分类和管理</li>
                    <li>个性化阅读设置</li>
                    <li>阅读进度同步</li>
                    <li>书籍封面自定义</li>
                    <li>阅读统计和分析</li>
                </ul>
                
                <h2>2. 用户权利与义务</h2>
                
                <h3>2.1 用户权利</h3>
                <ul>
                    <li>免费使用本应用的基本功能</li>
                    <li>享受优质的客户服务支持</li>
                    <li>对应用功能提出建议和反馈</li>
                    <li>在遵守法律法规的前提下自由使用本应用</li>
                </ul>
                
                <h3>2.2 用户义务</h3>
                <ul>
                    <li>遵守中华人民共和国相关法律法规</li>
                    <li>不得利用本应用从事违法活动</li>
                    <li>不得传播违法、有害、威胁、诽谤、骚扰、侵权等不良信息</li>
                    <li>不得恶意攻击、破坏本应用的正常运行</li>
                    <li>保护个人账户安全，不得将账户借给他人使用</li>
                </ul>
                
                <h2>3. 知识产权</h2>
                <p>本应用及其所有内容，包括但不限于软件、界面设计、文字、图片、音频、视频等，均受中华人民共和国著作权法、商标法、专利法等相关法律法规保护。</p>
                
                <div class="warning">
                    <p><strong>重要提醒：</strong></p>
                    <ul>
                        <li>用户上传的电子书文件应确保拥有合法版权</li>
                        <li>本应用仅提供阅读服务，不承担版权纠纷责任</li>
                        <li>用户应自行承担因使用侵权内容而产生的法律责任</li>
                    </ul>
                </div>
                
                <h2>4. 隐私保护</h2>
                <p>我们高度重视用户隐私保护，详细内容请参阅《隐私政策》。我们承诺：</p>
                <ul>
                    <li>仅在必要时收集用户信息</li>
                    <li>不会向第三方出售用户个人信息</li>
                    <li>采用行业标准的安全措施保护用户数据</li>
                    <li>用户可随时查看、修改或删除个人信息</li>
                </ul>
                
                <h2>5. 服务变更与终止</h2>
                <h3>5.1 服务变更</h3>
                <p>我们保留随时修改、升级或终止部分或全部服务的权利。重大变更将提前通知用户。</p>
                
                <h3>5.2 服务终止</h3>
                <p>在以下情况下，我们有权终止向用户提供服务：</p>
                <ul>
                    <li>用户违反本协议条款</li>
                    <li>用户从事违法活动</li>
                    <li>技术或商业原因导致服务无法继续</li>
                </ul>
                
                <h2>6. 免责声明</h2>
                <p>在法律允许的最大范围内，本应用对以下情况不承担责任：</p>
                <ul>
                    <li>因用户使用不当造成的任何损失</li>
                    <li>因第三方原因导致的服务中断</li>
                    <li>因不可抗力因素造成的服务中断</li>
                    <li>用户因使用本应用而产生的间接损失</li>
                </ul>
                
                <h2>7. 协议修改</h2>
                <p>我们有权随时修改本协议条款。修改后的协议将在应用内公布，用户继续使用本应用即视为同意修改后的协议。</p>
                
                <h2>8. 争议解决</h2>
                <p>因本协议产生的争议，双方应友好协商解决。协商不成的，可向有管辖权的人民法院提起诉讼。</p>
                
                <h2>9. 联系方式</h2>
                <p>如果您对本协议有任何疑问，请通过以下方式联系我们：</p>
                <ul>
                    <li>应用内反馈功能</li>
                    <li>邮箱：support@ibylin.com</li>
                </ul>
                
                <div class="footer">
                    <p>本协议最后更新时间：2024年9月5日</p>
                    <p>© 2024 iBylin. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
