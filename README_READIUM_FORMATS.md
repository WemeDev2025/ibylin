# Readium 支持的电子书格式

## 概述
Readium是一个开源的电子书阅读器引擎，支持多种电子书格式。我们的应用使用Readium 3.0.0版本。

## 官方支持的格式

### 1. EPUB (完全支持) ✅
- **格式**: `.epub`
- **支持级别**: 完全支持
- **功能**: 
  - 完整的EPUB 2.0和3.0支持
  - 元数据解析
  - 目录导航
  - 字体嵌入
  - 图片和多媒体
  - 样式表支持

### 2. PDF (部分支持) ⚠️
- **格式**: `.pdf`
- **支持级别**: 部分支持
- **功能**: 
  - 基本文本提取
  - 页面导航
  - 缩放和旋转
- **限制**: 
  - 需要额外的PDF适配器
  - 复杂布局可能有问题

### 3. CBZ/CBR (支持) ✅
- **格式**: `.cbz`, `.cbr`
- **支持级别**: 完全支持
- **功能**: 
  - 漫画和图像书籍
  - 压缩文件支持
  - 图像浏览

### 4. 纯文本 (支持) ✅
- **格式**: `.txt`
- **支持级别**: 完全支持
- **功能**: 
  - 基本文本显示
  - 编码检测

## 未来计划

### 短期目标 (1-3个月)
1. **PDF优化**: 集成PDFium适配器
2. **格式检测**: 改进文件格式自动识别
3. **性能优化**: 提升EPUB解析速度

### 长期目标 (3-6个月)
1. **更多格式**: 支持DOCX、RTF等格式
2. **格式转换**: 内置格式转换功能
3. **云同步**: 支持多种格式的云同步

## 技术架构

### 当前架构
```
BookScanner (统一扫描器)
├── EpubScanner (EPUB扫描)
└── 未来: PdfScanner, DocxScanner等

ReadiumHelper (统一阅读器)
├── EPUB解析 (Readium原生)
└── 未来: 其他格式解析
```

### 目标架构
```
BookScanner (统一扫描器)
├── EpubScanner (EPUB扫描)
├── PdfScanner (PDF扫描)
└── DocxScanner (DOCX扫描)

ReadiumHelper (统一阅读器)
├── EPUB解析 (Readium原生)
├── PDF解析 (Readium + PDFium)
└── 其他格式解析
```

## 依赖配置

### 当前依赖
```gradle
// Readium Kotlin Toolkit 3.0.0
implementation "org.readium.kotlin-toolkit:readium-shared:3.0.0"
implementation "org.readium.kotlin-toolkit:readium-streamer:3.0.0"
implementation "org.readium.kotlin-toolkit:readium-navigator:3.0.0"
implementation "org.readium.kotlin-toolkit:readium-opds:3.0.0"
implementation "org.readium.kotlin-toolkit:readium-lcp:3.0.0"

// 暂时移除PDF适配器，避免依赖冲突
// implementation "org.readium.kotlin-toolkit:readium-adapter-pdfium:3.0.0"
```

### 未来依赖
```gradle
// 添加PDF支持
implementation "org.readium.kotlin-toolkit:readium-adapter-pdfium:3.0.0"

// 添加MOBI支持 (当Readium支持时)
// implementation "org.readium.kotlin-toolkit:readium-adapter-mobi:3.0.0"
```

## 总结

### 当前状态
- ✅ EPUB: 完全支持
- ⚠️ PDF: 部分支持
- 🔄 MOBI: 实验性支持
- ✅ CBZ/CBR: 完全支持
- ✅ TXT: 完全支持

### 优势
1. **统一架构**: 所有格式使用相同的阅读器接口
2. **未来兼容**: 架构设计支持未来添加更多格式
3. **性能优化**: 使用Readium的原生解析器
4. **标准兼容**: 遵循国际电子书标准

### 注意事项
1. MOBI支持目前是实验性的
2. 复杂PDF文件可能有兼容性问题
3. 需要定期更新Readium依赖以获得最新功能

## 参考资料
- [Readium 官方文档](https://readium.org/)
- [Readium GitHub](https://github.com/readium)
- [Readium 3.0.0 发布说明](https://github.com/readium/readium-3/releases)
