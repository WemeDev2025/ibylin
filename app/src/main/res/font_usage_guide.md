# 字体使用说明

## MaShanZheng 字体

### 字体文件
- `mashanzheng_regular.ttf` - 马善政字体文件
- `mashanzheng_font.xml` - 字体资源定义文件

### 使用方法

#### 1. 在XML布局中使用
```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="使用马善政字体的文本"
    android:fontFamily="@font/mashanzheng_font" />
```

#### 2. 在代码中使用
```kotlin
// 方法1：通过字体资源ID
val typeface = ResourcesCompat.getFont(context, R.font.mashanzheng_font)
textView.typeface = typeface

// 方法2：通过字体文件
val typeface = Typeface.createFromAsset(context.assets, "fonts/mashanzheng_regular.ttf")
textView.typeface = typeface
```

#### 3. 在样式文件中使用
```xml
<style name="CustomTextStyle">
    <item name="android:fontFamily">@font/mashanzheng_font</item>
</style>
```

### 字体特点
- 马善政字体是一款优秀的中文字体
- 支持中文、英文等多种字符
- 适合用于标题、正文等文本显示

### 注意事项
- 字体文件大小：约5.6MB
- 确保在低内存设备上谨慎使用
- 建议在应用启动时预加载字体以提高性能
