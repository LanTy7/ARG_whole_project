#!/usr/bin/env python3
"""
ARG 网站性能监控脚本

使用方法：
1. 先在终端运行此脚本：python performance_monitor.py
2. 然后去网站提交分析任务
3. 任务完成后按 Ctrl+C 停止监控
4. 查看生成的 CSV 文件和统计报告

依赖安装：
pip install psutil GPUtil
"""

import psutil
import time
import csv
import os
import signal
import sys
from datetime import datetime

# 尝试导入 GPU 监控库
try:
    import GPUtil
    HAS_GPU = True
except ImportError:
    HAS_GPU = False
    print("[警告] 未安装 GPUtil，无法监控 GPU。安装命令：pip install GPUtil")

# 全局变量
running = True
data_records = []

def signal_handler(sig, frame):
    """处理 Ctrl+C 信号"""
    global running
    print("\n\n[停止] 正在生成报告...")
    running = False

def get_cpu_info():
    """获取 CPU 信息"""
    cpu_percent = psutil.cpu_percent(interval=0.1)
    cpu_per_core = psutil.cpu_percent(percpu=True)
    cores_active = sum(1 for c in cpu_per_core if c > 5)  # >5% 算活跃
    return cpu_percent, cores_active, len(cpu_per_core)

def get_memory_info():
    """获取内存信息"""
    mem = psutil.virtual_memory()
    return mem.used / 1024**3, mem.total / 1024**3, mem.percent

def get_gpu_info():
    """获取 GPU 信息"""
    if not HAS_GPU:
        return 0, 0, 0, 0
    
    try:
        gpus = GPUtil.getGPUs()
        if gpus:
            gpu = gpus[0]
            return gpu.memoryUsed, gpu.memoryTotal, gpu.load * 100, gpu.temperature
        return 0, 0, 0, 0
    except Exception:
        return 0, 0, 0, 0

def get_disk_io():
    """获取磁盘 IO"""
    try:
        io = psutil.disk_io_counters()
        return io.read_bytes, io.write_bytes
    except Exception:
        return 0, 0

def format_size(bytes_val):
    """格式化字节大小"""
    for unit in ['B', 'KB', 'MB', 'GB']:
        if bytes_val < 1024:
            return f"{bytes_val:.2f} {unit}"
        bytes_val /= 1024
    return f"{bytes_val:.2f} TB"

def print_status(record):
    """实时打印状态"""
    os.system('clear' if os.name == 'posix' else 'cls')
    
    print("=" * 60)
    print("       ARG 网站性能监控 (按 Ctrl+C 停止)")
    print("=" * 60)
    print(f"  时间: {record['timestamp']}")
    print(f"  已运行: {record['elapsed_seconds']:.0f} 秒")
    print("-" * 60)
    
    print(f"\n  [CPU]")
    print(f"    总使用率: {record['cpu_percent']:.1f}%")
    print(f"    活跃核心: {record['cpu_cores_active']}/{record['cpu_cores_total']}")
    
    print(f"\n  [内存]")
    print(f"    已用: {record['mem_used_gb']:.2f} GB / {record['mem_total_gb']:.2f} GB ({record['mem_percent']:.1f}%)")
    
    if HAS_GPU:
        print(f"\n  [GPU]")
        print(f"    显存: {record['gpu_mem_used_mb']:.0f} MB / {record['gpu_mem_total_mb']:.0f} MB")
        print(f"    利用率: {record['gpu_util_percent']:.1f}%")
        print(f"    温度: {record['gpu_temp']:.0f}°C")
    
    print(f"\n  [磁盘 IO] (本次监控期间)")
    print(f"    读取: {format_size(record['disk_read_total'])}")
    print(f"    写入: {format_size(record['disk_write_total'])}")
    
    print("\n" + "=" * 60)
    print(f"  数据已采样 {len(data_records)} 次")
    print("=" * 60)

def generate_report(filename, data_records):
    """生成统计报告"""
    if not data_records:
        print("[警告] 没有收集到数据")
        return
    
    # 计算统计数据
    cpu_values = [r['cpu_percent'] for r in data_records]
    mem_values = [r['mem_used_gb'] for r in data_records]
    gpu_mem_values = [r['gpu_mem_used_mb'] for r in data_records]
    gpu_util_values = [r['gpu_util_percent'] for r in data_records]
    
    report = f"""
================================================================================
                        性能监控报告
================================================================================

监控时间: {data_records[0]['timestamp']} ~ {data_records[-1]['timestamp']}
监控时长: {data_records[-1]['elapsed_seconds']:.0f} 秒
采样次数: {len(data_records)}

--------------------------------------------------------------------------------
CPU 使用情况
--------------------------------------------------------------------------------
  平均使用率: {sum(cpu_values)/len(cpu_values):.1f}%
  最大使用率: {max(cpu_values):.1f}%
  最小使用率: {min(cpu_values):.1f}%

--------------------------------------------------------------------------------
内存使用情况
--------------------------------------------------------------------------------
  平均使用: {sum(mem_values)/len(mem_values):.2f} GB
  峰值使用: {max(mem_values):.2f} GB
  最小使用: {min(mem_values):.2f} GB
  总内存: {data_records[0]['mem_total_gb']:.2f} GB

--------------------------------------------------------------------------------
GPU 使用情况
--------------------------------------------------------------------------------
  平均显存: {sum(gpu_mem_values)/len(gpu_mem_values):.0f} MB
  峰值显存: {max(gpu_mem_values):.0f} MB
  平均利用率: {sum(gpu_util_values)/len(gpu_util_values):.1f}%
  峰值利用率: {max(gpu_util_values):.1f}%

--------------------------------------------------------------------------------
磁盘 IO
--------------------------------------------------------------------------------
  总读取: {format_size(data_records[-1]['disk_read_total'])}
  总写入: {format_size(data_records[-1]['disk_write_total'])}

================================================================================
详细数据已保存到: {filename}
================================================================================
"""
    
    print(report)
    
    # 保存报告到文件
    report_filename = filename.replace('.csv', '_report.txt')
    with open(report_filename, 'w', encoding='utf-8') as f:
        f.write(report)
    print(f"[完成] 报告已保存到: {report_filename}")

def main():
    global running, data_records
    
    # 注册信号处理
    signal.signal(signal.SIGINT, signal_handler)
    
    # 创建输出文件
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    filename = f"performance_log_{timestamp}.csv"
    
    print(f"\n[启动] 性能监控已开始")
    print(f"[文件] 数据将保存到: {filename}")
    print(f"[提示] 现在可以去网站提交分析任务了")
    print(f"[提示] 任务完成后按 Ctrl+C 停止监控并生成报告\n")
    time.sleep(3)
    
    # 记录初始磁盘 IO
    disk_read_start, disk_write_start = get_disk_io()
    start_time = time.time()
    
    # CSV 表头
    headers = [
        'timestamp', 'elapsed_seconds',
        'cpu_percent', 'cpu_cores_active', 'cpu_cores_total',
        'mem_used_gb', 'mem_total_gb', 'mem_percent',
        'gpu_mem_used_mb', 'gpu_mem_total_mb', 'gpu_util_percent', 'gpu_temp',
        'disk_read_total', 'disk_write_total'
    ]
    
    with open(filename, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=headers)
        writer.writeheader()
        
        while running:
            # 收集数据
            cpu_percent, cpu_cores_active, cpu_cores_total = get_cpu_info()
            mem_used_gb, mem_total_gb, mem_percent = get_memory_info()
            gpu_mem_used, gpu_mem_total, gpu_util, gpu_temp = get_gpu_info()
            disk_read, disk_write = get_disk_io()
            
            record = {
                'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
                'elapsed_seconds': time.time() - start_time,
                'cpu_percent': cpu_percent,
                'cpu_cores_active': cpu_cores_active,
                'cpu_cores_total': cpu_cores_total,
                'mem_used_gb': mem_used_gb,
                'mem_total_gb': mem_total_gb,
                'mem_percent': mem_percent,
                'gpu_mem_used_mb': gpu_mem_used,
                'gpu_mem_total_mb': gpu_mem_total,
                'gpu_util_percent': gpu_util,
                'gpu_temp': gpu_temp,
                'disk_read_total': disk_read - disk_read_start,
                'disk_write_total': disk_write - disk_write_start
            }
            
            # 保存数据
            data_records.append(record)
            writer.writerow(record)
            f.flush()
            
            # 打印实时状态
            print_status(record)
            
            # 等待下一次采样（2秒间隔）
            time.sleep(2)
    
    # 生成报告
    generate_report(filename, data_records)

if __name__ == "__main__":
    main()
