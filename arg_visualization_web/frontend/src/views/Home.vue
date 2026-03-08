<template>
  <div class="home-container">
    <el-card class="welcome-card">
      <div class="welcome-content" :style="{ backgroundImage: welcomeUrl ? `url(${welcomeUrl})` : undefined }">
        <img :src="logoUrl" alt="" class="icon" />
        <h1>{{ $t('home.welcome') }}</h1>
        <p class="subtitle">{{ $t('home.subtitle') }}</p>
        
        <div class="actions">
          <el-button type="primary" size="large" @click="router.push('/upload')">
            <el-icon><Upload /></el-icon>
            {{ $t('home.startUpload') }}
          </el-button>
          <el-button size="large" @click="router.push('/history')">
            <el-icon><Clock /></el-icon>
            {{ $t('home.viewHistory') }}
          </el-button>
        </div>
      </div>
    </el-card>
    
    <!-- 快速统计 -->
    <el-row :gutter="24" class="stats-row">
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="color: var(--theme-status-success);"><Document /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalFiles }}</div>
              <div class="stat-label">{{ $t('home.stats.totalFiles') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="color: var(--theme-status-success);"><Checked /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.completedTasks }}</div>
              <div class="stat-label">{{ $t('home.stats.completedTasks') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="color: var(--theme-stat-tasks);"><Loading /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.runningTasks }}</div>
              <div class="stat-label">{{ $t('home.stats.runningTasks') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Upload, Clock, Document, Checked, Loading } from '@element-plus/icons-vue';
import logoUrl from '@/assets/arg.svg';
import welcomeUrl from '@/assets/welcome.png';
import { getUserFiles } from '@/api/file';
import { getUserTasks } from '@/api/task';

const router = useRouter();

const stats = ref({
  totalFiles: 0,
  completedTasks: 0,
  runningTasks: 0,
});

// 获取统计数据
const fetchStats = async () => {
  try {
    const [filesRes, tasksRes] = await Promise.all([
      getUserFiles(),
      getUserTasks(),
    ]);
    
    stats.value.totalFiles = filesRes.data.length;
    
    const tasks = tasksRes.data;
    stats.value.completedTasks = tasks.filter(t => t.status === 'COMPLETED').length;
    stats.value.runningTasks = tasks.filter(t => t.status === 'RUNNING').length;
  } catch (error) {
    console.error('Failed to fetch stats:', error);
    // 使用模拟数据进行预览
    stats.value.totalFiles = 5;
    stats.value.completedTasks = 3;
    stats.value.runningTasks = 1;
  }
};

onMounted(() => {
  fetchStats();
});
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-card {
  margin-bottom: 24px;
  background: linear-gradient(135deg, #e8f6f5 0%, #f0f9f8 50%, #f5fbfa 100%);
  border: 1px solid rgba(42, 157, 143, 0.15);
  box-shadow: 0 8px 32px rgba(42, 157, 143, 0.1);
}

:deep(.welcome-card .el-card__body) {
  padding: 0;
}

.welcome-content {
  text-align: center;
  padding: 80px 40px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fdfc 100%);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
  overflow: hidden;
}

.welcome-content::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 20% 50%, rgba(42, 157, 143, 0.05) 0%, transparent 50%),
              radial-gradient(circle at 80% 80%, rgba(61, 204, 199, 0.05) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.welcome-content::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: repeating-linear-gradient(
    45deg,
    transparent,
    transparent 10px,
    rgba(42, 157, 143, 0.02) 10px,
    rgba(42, 157, 143, 0.02) 20px
  );
  animation: slide 20s linear infinite;
}

@keyframes slide {
  0% { transform: translate(0, 0); }
  100% { transform: translate(50px, 50px); }
}

.icon {
  width: 120px;
  height: 120px;
  object-fit: contain;
  margin-bottom: 32px;
  filter: drop-shadow(0 2px 12px var(--theme-shadow-4));
  animation: float 3s ease-in-out infinite, pulse 2s ease-in-out infinite;
  position: relative;
  z-index: 1;
  display: inline-block;
}

@keyframes float {
  0%, 100% { transform: translateY(0px) rotate(0deg); }
  50% { transform: translateY(-15px) rotate(5deg); }
}

@keyframes pulse {
  0%, 100% { filter: drop-shadow(0 2px 12px var(--theme-shadow-4)); }
  50% { filter: drop-shadow(0 4px 20px rgba(var(--theme-shadow-rgb), 0.35)); }
}

.welcome-content h1 {
  font-size: 42px;
  margin: 0 0 20px 0;
  color: #1a2744;
  font-weight: 600;
  font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  text-shadow: 0 2px 8px rgba(255, 255, 255, 0.6), 0 1px 3px rgba(0, 0, 0, 0.15);
  position: relative;
  z-index: 1;
}

.welcome-content .subtitle {
  font-size: 18px;
  color: rgba(26, 39, 68, 0.85);
  margin: 0 0 50px 0;
  position: relative;
  z-index: 1;
  font-weight: 400;
  letter-spacing: 0.04em;
  text-shadow: 0 1px 4px rgba(255, 255, 255, 0.5);
}

.actions {
  display: flex;
  justify-content: center;
  gap: 24px;
  position: relative;
  z-index: 1;
  flex-wrap: wrap;
}

.actions :deep(.el-button) {
  padding: 16px 40px;
  font-size: 17px;
  font-weight: 600;
  border-radius: 12px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.actions :deep(.el-button)::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
}

.actions :deep(.el-button:hover::before) {
  width: 300px;
  height: 300px;
}

.actions :deep(.el-button--primary) {
  background: linear-gradient(135deg, #2a9d8f 0%, #3dccc7 100%);
  border: 1px solid #238b7e;
  color: #ffffff;
  box-shadow: 0 4px 16px rgba(42, 157, 143, 0.25);
}

.actions :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #238b7e 0%, #2a9d8f 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(42, 157, 143, 0.35);
}

.actions :deep(.el-button--default) {
  background: linear-gradient(135deg, #ffffff 0%, #f8fdfc 100%);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(42, 157, 143, 0.3);
  color: #2a9d8f;
  box-shadow: 0 2px 12px rgba(42, 157, 143, 0.1);
}

.actions :deep(.el-button--default:hover) {
  background: linear-gradient(135deg, #f0f9f8 0%, #e8f6f5 100%);
  border-color: rgba(42, 157, 143, 0.4);
  color: #238b7e;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(42, 157, 143, 0.2);
}

.stats-row {
  margin-top: 32px;
}

.stat-card {
  cursor: default;
  border: 1px solid rgba(42, 157, 143, 0.12);
  border-radius: 16px;
  box-shadow: 0 6px 20px rgba(42, 157, 143, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.5);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(135deg, #ffffff 0%, #f8fdfc 100%);
  backdrop-filter: blur(10px);
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(42, 157, 143, 0.06), transparent);
  transition: left 0.6s;
}

.stat-card:hover::before {
  left: 100%;
}

:deep(.stat-card .el-card__body) {
  background: transparent;
}

.stat-card:hover {
  transform: translateY(-8px) scale(1.02);
  border-color: rgba(42, 157, 143, 0.25);
  box-shadow: 0 12px 32px rgba(42, 157, 143, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 24px 20px;
  position: relative;
  z-index: 1;
}

.stat-icon {
  font-size: 56px;
  margin-right: 24px;
  filter: drop-shadow(0 0 15px currentColor);
  transition: all 0.4s;
}

.stat-card:hover .stat-icon {
  transform: scale(1.15) rotate(5deg);
  filter: drop-shadow(0 0 20px currentColor);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 42px;
  font-weight: 700;
  color: #2a9d8f;
  text-shadow: 0 1px 2px rgba(42, 157, 143, 0.15);
  line-height: 1;
  margin-bottom: 10px;
  transition: all 0.4s;
  background: linear-gradient(135deg, #2a9d8f 0%, #3dccc7 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-card:hover .stat-value {
  transform: scale(1.1);
}

.stat-label {
  font-size: 15px;
  color: var(--theme-text-muted);
  font-weight: 500;
  letter-spacing: 0.5px;
}

.home-container {
  padding: 20px;
  animation: fadeIn 0.8s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
