#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
分析EPUB封面图片的尺寸和比例
"""

import os
from PIL import Image
from pathlib import Path
import statistics

def analyze_cover_images(cover_dir):
    """分析封面图片的尺寸和比例"""
    
    cover_dir = Path(cover_dir)
    if not cover_dir.exists():
        print(f"目录不存在: {cover_dir}")
        return
    
    # 支持的图片格式
    image_extensions = {'.jpg', '.jpeg', '.png', '.bmp', '.gif'}
    
    # 收集所有图片信息
    image_info = []
    
    for image_file in cover_dir.iterdir():
        if image_file.suffix.lower() in image_extensions:
            try:
                with Image.open(image_file) as img:
                    width, height = img.size
                    aspect_ratio = width / height
                    
                    image_info.append({
                        'filename': image_file.name,
                        'width': width,
                        'height': height,
                        'aspect_ratio': aspect_ratio,
                        'format': img.format,
                        'mode': img.mode
                    })
                    
                    print(f"✓ {image_file.name}: {width}x{height} (比例: {aspect_ratio:.3f})")
                    
            except Exception as e:
                print(f"✗ {image_file.name}: 读取失败 - {e}")
    
    if not image_info:
        print("没有找到可分析的图片文件")
        return
    
    # 分析统计信息
    print("\n" + "="*60)
    print("封面图片尺寸分析报告")
    print("="*60)
    
    # 基本统计
    widths = [info['width'] for info in image_info]
    heights = [info['height'] for info in image_info]
    ratios = [info['aspect_ratio'] for info in image_info]
    
    print(f"总图片数量: {len(image_info)}")
    print(f"图片格式分布: {', '.join(set(info['format'] for info in image_info))}")
    
    # 尺寸统计
    print(f"\n宽度统计:")
    print(f"  最小宽度: {min(widths)}px")
    print(f"  最大宽度: {max(widths)}px")
    print(f"  平均宽度: {statistics.mean(widths):.1f}px")
    print(f"  中位数宽度: {statistics.median(widths):.1f}px")
    
    print(f"\n高度统计:")
    print(f"  最小高度: {min(heights)}px")
    print(f"  最大高度: {max(heights)}px")
    print(f"  平均高度: {statistics.mean(heights):.1f}px")
    print(f"  中位数高度: {statistics.median(heights):.1f}px")
    
    # 比例统计
    print(f"\n宽高比统计:")
    print(f"  最小比例: {min(ratios):.3f}")
    print(f"  最大比例: {max(ratios):.3f}")
    print(f"  平均比例: {statistics.mean(ratios):.3f}")
    print(f"  中位数比例: {statistics.median(ratios):.3f}")
    
    # 比例分布分析
    print(f"\n比例分布分析:")
    portrait_count = sum(1 for r in ratios if r < 0.8)  # 竖版
    square_count = sum(1 for r in ratios if 0.8 <= r <= 1.2)  # 方形
    landscape_count = sum(1 for r in ratios if r > 1.2)  # 横版
    
    print(f"  竖版封面 (比例 < 0.8): {portrait_count} 本 ({portrait_count/len(ratios)*100:.1f}%)")
    print(f"  方形封面 (0.8 ≤ 比例 ≤ 1.2): {square_count} 本 ({square_count/len(ratios)*100:.1f}%)")
    print(f"  横版封面 (比例 > 1.2): {landscape_count} 本 ({landscape_count/len(ratios)*100:.1f}%)")
    
    # 推荐比例
    print(f"\n推荐封面比例设置:")
    print(f"  基于中位数比例: {statistics.median(ratios):.3f}")
    print(f"  基于平均比例: {statistics.mean(ratios):.3f}")
    
    # 计算兼容性比例
    min_ratio = min(ratios)
    max_ratio = max(ratios)
    
    print(f"\n兼容性分析:")
    print(f"  当前比例范围: {min_ratio:.3f} - {max_ratio:.3f}")
    print(f"  建议设置比例: {min_ratio:.3f} (确保所有封面都能完整显示)")
    print(f"  或者使用: 0.7 (向下取整，提供额外边距)")
    
    # 具体建议
    print(f"\n具体建议:")
    if min_ratio < 0.7:
        print(f"  由于存在竖版封面 (最小比例: {min_ratio:.3f})")
        print(f"  建议设置封面容器比例为: {min_ratio:.3f}")
        print(f"  或者使用更保守的: 0.65")
    else:
        print(f"  所有封面比例都在合理范围内")
        print(f"  建议设置封面容器比例为: {min_ratio:.3f}")
    
    # 显示一些具体例子
    print(f"\n具体例子:")
    sorted_by_ratio = sorted(image_info, key=lambda x: x['aspect_ratio'])
    
    print(f"  最窄的封面:")
    for info in sorted_by_ratio[:3]:
        print(f"    {info['filename']}: {info['width']}x{info['height']} (比例: {info['aspect_ratio']:.3f})")
    
    print(f"  最宽的封面:")
    for info in sorted_by_ratio[-3:]:
        print(f"    {info['filename']}: {info['width']}x{info['height']} (比例: {info['aspect_ratio']:.3f})")

def main():
    # 分析封面图片
    cover_dir = "epub_covers 2"
    analyze_cover_images(cover_dir)

if __name__ == "__main__":
    main()
