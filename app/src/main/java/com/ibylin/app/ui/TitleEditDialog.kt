package com.ibylin.app.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.ibylin.app.R
import com.ibylin.app.utils.TitleColor
import com.ibylin.app.utils.TitleFont

/**
 * 书名位置枚举
 */
enum class TitlePosition {
    TOP,    // 上方
    CENTER, // 中间
    BOTTOM, // 下方
    LEFT,   // 左侧
    RIGHT   // 右侧
}

/**
 * 书名布局枚举
 */
enum class TitleLayout {
    HORIZONTAL, // 水平布局
    VERTICAL    // 垂直布局
}

/**
 * M3风格的标题编辑对话框
 */
class TitleEditDialog : DialogFragment() {
    
    private var onTitleConfirmed: ((String, TitlePosition, TitleLayout, TitleColor, TitleFont) -> Unit)? = null
    private var defaultTitle: String = ""
    private var originalDefaultTitle: String = "" // 保存原始的完整默认书名
    
    private lateinit var etTitle: EditText
    private lateinit var btnConfirm: Button
    private lateinit var btnCancel: Button
    private lateinit var tvCharCount: TextView
    
    // 位置选项按钮
    private lateinit var btnPositionTop: com.google.android.material.button.MaterialButton
    private lateinit var btnPositionCenter: com.google.android.material.button.MaterialButton
    private lateinit var btnPositionBottom: com.google.android.material.button.MaterialButton
    private lateinit var btnPositionLeft: com.google.android.material.button.MaterialButton
    private lateinit var btnPositionRight: com.google.android.material.button.MaterialButton
    
    // 布局选项按钮
    private lateinit var btnLayoutHorizontal: com.google.android.material.button.MaterialButton
    private lateinit var btnLayoutVertical: com.google.android.material.button.MaterialButton
    
    // 颜色选项按钮
    private lateinit var btnColorBlack: com.google.android.material.button.MaterialButton
    private lateinit var btnColorWhite: com.google.android.material.button.MaterialButton
    
    // 字体选项按钮
    private lateinit var btnFontSystem: com.google.android.material.button.MaterialButton
    private lateinit var btnFontMashanzheng: com.google.android.material.button.MaterialButton
    private lateinit var btnFontNotoserif: com.google.android.material.button.MaterialButton
    
    // 当前选中的选项
    private var selectedPosition = TitlePosition.CENTER
    private var selectedLayout = TitleLayout.HORIZONTAL
    private var selectedColor = TitleColor.BLACK
    private var selectedFont = TitleFont.SYSTEM
    
    companion object {
        private const val ARG_DEFAULT_TITLE = "default_title"
        private const val MAX_CHAR_COUNT = 15
        
        fun newInstance(defaultTitle: String): TitleEditDialog {
            val dialog = TitleEditDialog()
            val args = Bundle()
            args.putString(ARG_DEFAULT_TITLE, defaultTitle)
            dialog.arguments = args
            return dialog
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        originalDefaultTitle = arguments?.getString(ARG_DEFAULT_TITLE) ?: ""
        defaultTitle = originalDefaultTitle
        Log.d("TitleEditDialog", "onCreate: originalDefaultTitle = $originalDefaultTitle")
        Log.d("TitleEditDialog", "onCreate: defaultTitle = $defaultTitle")
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d("TitleEditDialog", "onCreateDialog: 开始创建对话框")
        
        return try {
            // 确保defaultTitle被正确初始化
            defaultTitle = arguments?.getString(ARG_DEFAULT_TITLE) ?: ""
            Log.d("TitleEditDialog", "onCreateDialog: defaultTitle = $defaultTitle")
            
            val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_title_edit, null)
            Log.d("TitleEditDialog", "onCreateDialog: 布局文件加载成功")
            
            initViews(view)
            Log.d("TitleEditDialog", "onCreateDialog: 视图初始化完成")
            
            setupListeners()
            Log.d("TitleEditDialog", "onCreateDialog: 监听器设置完成")
            
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(view)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            
            Log.d("TitleEditDialog", "onCreateDialog: 对话框创建完成")
            dialog
        } catch (e: Exception) {
            Log.e("TitleEditDialog", "onCreateDialog: 创建对话框失败", e)
            // 返回一个简单的对话框作为fallback
            val fallbackDialog = Dialog(requireContext())
            fallbackDialog.setTitle("错误")
            val textView = android.widget.TextView(requireContext())
            textView.text = "无法创建编辑对话框: ${e.message}"
            textView.setPadding(50, 50, 50, 50)
            fallbackDialog.setContentView(textView)
            fallbackDialog
        }
    }
    
    private fun initViews(view: View) {
        try {
            Log.d("TitleEditDialog", "initViews: 开始初始化视图")
            
            etTitle = view.findViewById(R.id.et_title)
            btnConfirm = view.findViewById(R.id.btn_confirm)
            btnCancel = view.findViewById(R.id.btn_cancel)
            tvCharCount = view.findViewById(R.id.tv_char_count)
            
            Log.d("TitleEditDialog", "initViews: 基本视图组件初始化完成")
            
            // 位置选项按钮
            btnPositionTop = view.findViewById(R.id.btn_position_top)
            btnPositionCenter = view.findViewById(R.id.btn_position_center)
            btnPositionBottom = view.findViewById(R.id.btn_position_bottom)
            btnPositionLeft = view.findViewById(R.id.btn_position_left)
            btnPositionRight = view.findViewById(R.id.btn_position_right)
            
            Log.d("TitleEditDialog", "initViews: 位置按钮初始化完成")
            
        // 布局选项按钮
        btnLayoutHorizontal = view.findViewById(R.id.btn_layout_horizontal)
        btnLayoutVertical = view.findViewById(R.id.btn_layout_vertical)
        
        // 颜色选项按钮
        btnColorBlack = view.findViewById(R.id.btn_color_black)
        btnColorWhite = view.findViewById(R.id.btn_color_white)
        
        // 字体选项按钮
        btnFontSystem = view.findViewById(R.id.btn_font_system)
        btnFontMashanzheng = view.findViewById(R.id.btn_font_mashanzheng)
        btnFontNotoserif = view.findViewById(R.id.btn_font_notoserif)
            
            Log.d("TitleEditDialog", "initViews: 布局按钮初始化完成")
            
            // 设置默认标题
            etTitle.setText(defaultTitle)
            // 安全地设置光标位置，确保不超过文本长度
            val textLength = etTitle.text.length
            if (textLength > 0) {
                etTitle.setSelection(textLength)
            }
            
            Log.d("TitleEditDialog", "initViews: 默认标题设置完成: $defaultTitle")
            
            // 更新字符计数
            updateCharCount(etTitle.text.length)
            
        // 初始状态：如果有默认书名，确认按钮启用
        btnConfirm.isEnabled = originalDefaultTitle.trim().isNotEmpty()
            
            // 设置默认选中状态
            updatePositionButtons()
            updateLayoutButtons()
            updateColorButtons()
            updateFontButtons()
            
            Log.d("TitleEditDialog", "initViews: 视图初始化完成")
        } catch (e: Exception) {
            Log.e("TitleEditDialog", "initViews: 视图初始化失败", e)
            throw e
        }
    }
    
    private fun setupListeners() {
        // 标题输入监听
        etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""
                updateCharCount(text.length)
                // 如果用户输入了内容，使用用户输入；否则使用原始默认书名
                btnConfirm.isEnabled = text.trim().isNotEmpty() || originalDefaultTitle.trim().isNotEmpty()
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // 位置选项按钮监听器
        btnPositionTop.setOnClickListener {
            selectedPosition = TitlePosition.TOP
            updatePositionButtons()
        }
        
        btnPositionCenter.setOnClickListener {
            selectedPosition = TitlePosition.CENTER
            updatePositionButtons()
        }
        
        btnPositionBottom.setOnClickListener {
            selectedPosition = TitlePosition.BOTTOM
            updatePositionButtons()
        }
        
        btnPositionLeft.setOnClickListener {
            selectedPosition = TitlePosition.LEFT
            updatePositionButtons()
        }
        
        btnPositionRight.setOnClickListener {
            selectedPosition = TitlePosition.RIGHT
            updatePositionButtons()
        }
        
        // 布局选项按钮监听器
        btnLayoutHorizontal.setOnClickListener {
            selectedLayout = TitleLayout.HORIZONTAL
            updateLayoutButtons()
            updatePositionButtons() // 切换布局时更新位置按钮
        }
        
        btnLayoutVertical.setOnClickListener {
            selectedLayout = TitleLayout.VERTICAL
            Log.d("TitleEditDialog", "垂直布局按钮点击: selectedLayout = $selectedLayout")
            updateLayoutButtons()
            updatePositionButtons() // 切换布局时更新位置按钮
        }
        
        // 颜色选项按钮监听器
        btnColorBlack.setOnClickListener {
            selectedColor = TitleColor.BLACK
            updateColorButtons()
        }
        
        btnColorWhite.setOnClickListener {
            selectedColor = TitleColor.WHITE
            updateColorButtons()
        }
        
        // 字体选项按钮监听器
        btnFontSystem.setOnClickListener {
            selectedFont = TitleFont.SYSTEM
            updateFontButtons()
        }
        
        btnFontMashanzheng.setOnClickListener {
            selectedFont = TitleFont.MASHANZHENG
            updateFontButtons()
        }
        
        btnFontNotoserif.setOnClickListener {
            selectedFont = TitleFont.NOTOSERIF
            updateFontButtons()
        }
        
        // 确认按钮
        btnConfirm.setOnClickListener {
            val userInput = etTitle.text.toString().trim()
            // 如果用户输入了内容，使用用户输入；否则使用原始的完整默认书名
            val finalTitle = if (userInput.isNotEmpty()) userInput else originalDefaultTitle.trim()
            
            if (finalTitle.isNotEmpty()) {
                Log.d("TitleEditDialog", "确认按钮点击: 最终书名='$finalTitle', 用户输入='$userInput', 原始默认书名='$originalDefaultTitle'")
                Log.d("TitleEditDialog", "确认按钮点击: selectedPosition=$selectedPosition, selectedLayout=$selectedLayout, selectedColor=$selectedColor, selectedFont=$selectedFont")
                onTitleConfirmed?.invoke(finalTitle, selectedPosition, selectedLayout, selectedColor, selectedFont)
                dismiss()
            }
        }
        
        // 取消按钮
        btnCancel.setOnClickListener {
            dismiss()
        }
    }
    
    private fun updateCharCount(count: Int) {
        tvCharCount.text = "$count/15"
        tvCharCount.setTextColor(
            if (count > 15) {
                Color.parseColor("#B3261E") // 错误颜色
            } else {
                Color.parseColor("#49454F") // 正常颜色
            }
        )
    }
    
    fun setOnTitleConfirmedListener(listener: (String, TitlePosition, TitleLayout, TitleColor, TitleFont) -> Unit) {
        onTitleConfirmed = listener
    }
    
    /**
     * 更新位置按钮的选中状态
     */
    private fun updatePositionButtons() {
        // 根据布局类型显示/隐藏相应的位置按钮
        if (selectedLayout == TitleLayout.HORIZONTAL) {
            // 水平布局：显示上/中/下
            btnPositionTop.visibility = View.VISIBLE
            btnPositionCenter.visibility = View.VISIBLE
            btnPositionBottom.visibility = View.VISIBLE
            btnPositionLeft.visibility = View.GONE
            btnPositionRight.visibility = View.GONE
            
            // 重置所有按钮为未选中状态
            btnPositionTop.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
            btnPositionCenter.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
            btnPositionBottom.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
            btnPositionTop.setTextColor(Color.parseColor("#E0E0E0"))
            btnPositionCenter.setTextColor(Color.parseColor("#E0E0E0"))
            btnPositionBottom.setTextColor(Color.parseColor("#E0E0E0"))
            
            // 设置选中按钮
            when (selectedPosition) {
                TitlePosition.TOP -> {
                    btnPositionTop.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                    btnPositionTop.setTextColor(Color.WHITE)
                }
                TitlePosition.CENTER -> {
                    btnPositionCenter.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                    btnPositionCenter.setTextColor(Color.WHITE)
                }
                TitlePosition.BOTTOM -> {
                    btnPositionBottom.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                    btnPositionBottom.setTextColor(Color.WHITE)
                }
                else -> {
                    // 如果当前选中的是左/右，切换到中
                    selectedPosition = TitlePosition.CENTER
                    btnPositionCenter.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                    btnPositionCenter.setTextColor(Color.WHITE)
                }
            }
        } else {
            // 垂直布局：显示左/中/右（水平方向的位置）
            btnPositionTop.visibility = View.GONE
            btnPositionCenter.visibility = View.VISIBLE
            btnPositionBottom.visibility = View.GONE
            btnPositionLeft.visibility = View.VISIBLE
            btnPositionRight.visibility = View.VISIBLE
            
            // 重置所有按钮为未选中状态
            btnPositionLeft.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
            btnPositionCenter.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
            btnPositionRight.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
            btnPositionLeft.setTextColor(Color.parseColor("#E0E0E0"))
            btnPositionCenter.setTextColor(Color.parseColor("#E0E0E0"))
            btnPositionRight.setTextColor(Color.parseColor("#E0E0E0"))
            
            // 设置选中按钮
            when (selectedPosition) {
                TitlePosition.LEFT -> {
                    btnPositionLeft.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                    btnPositionLeft.setTextColor(Color.WHITE)
                }
                TitlePosition.CENTER -> {
                    btnPositionCenter.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                    btnPositionCenter.setTextColor(Color.WHITE)
                }
                TitlePosition.RIGHT -> {
                    btnPositionRight.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                    btnPositionRight.setTextColor(Color.WHITE)
                }
                else -> {
                    // 如果当前选中的是上/下，切换到中
                    selectedPosition = TitlePosition.CENTER
                    btnPositionCenter.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                    btnPositionCenter.setTextColor(Color.WHITE)
                }
            }
        }
    }
    
    /**
     * 更新布局按钮的选中状态
     */
    private fun updateLayoutButtons() {
        // 重置所有按钮
        btnLayoutHorizontal.backgroundTintList = null
        btnLayoutVertical.backgroundTintList = null
        
        // 设置选中按钮
        when (selectedLayout) {
            TitleLayout.HORIZONTAL -> {
                btnLayoutHorizontal.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                btnLayoutHorizontal.setTextColor(Color.WHITE)
                // 设置未选中按钮样式
                btnLayoutVertical.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
                btnLayoutVertical.setTextColor(Color.parseColor("#E0E0E0"))
            }
            TitleLayout.VERTICAL -> {
                btnLayoutVertical.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                btnLayoutVertical.setTextColor(Color.WHITE)
                // 设置未选中按钮样式
                btnLayoutHorizontal.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
                btnLayoutHorizontal.setTextColor(Color.parseColor("#E0E0E0"))
            }
        }
    }
    
    /**
     * 更新颜色按钮的选中状态
     */
    private fun updateColorButtons() {
        // 重置所有按钮
        btnColorBlack.backgroundTintList = null
        btnColorWhite.backgroundTintList = null
        
        // 设置选中按钮
        when (selectedColor) {
            TitleColor.BLACK -> {
                btnColorBlack.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                btnColorBlack.setTextColor(Color.WHITE)
                // 设置未选中按钮样式
                btnColorWhite.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
                btnColorWhite.setTextColor(Color.parseColor("#E0E0E0"))
            }
            TitleColor.WHITE -> {
                btnColorWhite.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                btnColorWhite.setTextColor(Color.WHITE)
                // 设置未选中按钮样式
                btnColorBlack.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
                btnColorBlack.setTextColor(Color.parseColor("#E0E0E0"))
            }
        }
    }
    
    /**
     * 更新字体按钮的选中状态
     */
    private fun updateFontButtons() {
        // 重置所有按钮
        btnFontSystem.backgroundTintList = null
        btnFontMashanzheng.backgroundTintList = null
        btnFontNotoserif.backgroundTintList = null
        
        // 设置选中按钮
        when (selectedFont) {
            TitleFont.SYSTEM -> {
                btnFontSystem.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                btnFontSystem.setTextColor(Color.WHITE)
                // 设置未选中按钮样式
                btnFontMashanzheng.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
                btnFontMashanzheng.setTextColor(Color.parseColor("#E0E0E0"))
                btnFontNotoserif.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
                btnFontNotoserif.setTextColor(Color.parseColor("#E0E0E0"))
            }
            TitleFont.MASHANZHENG -> {
                btnFontMashanzheng.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                btnFontMashanzheng.setTextColor(Color.WHITE)
                // 设置未选中按钮样式
                btnFontSystem.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
                btnFontSystem.setTextColor(Color.parseColor("#E0E0E0"))
                btnFontNotoserif.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
                btnFontNotoserif.setTextColor(Color.parseColor("#E0E0E0"))
            }
            TitleFont.NOTOSERIF -> {
                btnFontNotoserif.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                btnFontNotoserif.setTextColor(Color.WHITE)
                // 设置未选中按钮样式
                btnFontSystem.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
                btnFontSystem.setTextColor(Color.parseColor("#E0E0E0"))
                btnFontMashanzheng.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
                btnFontMashanzheng.setTextColor(Color.parseColor("#E0E0E0"))
            }
        }
    }
}
