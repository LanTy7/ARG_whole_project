<template>
  <div class="layout-container">
    <!-- 左侧侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="header-top">
          <img :src="logoUrl" alt="Logo" class="logo" />
          <LangSwitch class="lang-switch" />
        </div>
        <div class="title">
          <h2>{{ $t('sidebar.title') }}</h2>
          <p>{{ $t('sidebar.subtitle') }}</p>
        </div>
      </div>

      <el-menu
        :default-active="currentRoute"
        class="sidebar-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <span>{{ $t('nav.home') }}</span>
        </el-menu-item>

        <el-menu-item index="/upload">
          <el-icon><Upload /></el-icon>
          <span>{{ $t('nav.upload') }}</span>
        </el-menu-item>

        <el-menu-item index="/visualization">
          <el-icon><DataLine /></el-icon>
          <span>{{ $t('nav.visualization') }}</span>
        </el-menu-item>

        <el-menu-item index="/history">
          <el-icon><Clock /></el-icon>
          <span>{{ $t('nav.history') }}</span>
        </el-menu-item>

        <el-menu-item index="/introduction">
          <el-icon><InfoFilled /></el-icon>
          <span>{{ $t('nav.introduction') }}</span>
        </el-menu-item>


        <el-menu-item index="/admin" v-if="isAdmin">
          <el-icon><Setting /></el-icon>
          <span>{{ $t('nav.admin') }}</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <el-button
          v-if="!userStore.token"
          type="primary"
          size="large"
          class="auth-button"
          @click="handleLogin"
        >
          <el-icon><User /></el-icon>
          {{ $t('nav.login') }}
        </el-button>
        <el-button
          v-else
          type="danger"
          size="large"
          class="auth-button"
          @click="handleLogout"
        >
          <el-icon><SwitchButton /></el-icon>
          {{ $t('nav.logout') }}
        </el-button>
      </div>
    </aside>

    <!-- 右侧内容区 -->
    <main class="main-content" :style="{ backgroundImage: mainBgUrl ? `url(${mainBgUrl})` : undefined }">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import logoUrl from '@/assets/arg.svg'
import mainBgUrl from '@/assets/main-background.png'
import { ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
import {
  HomeFilled,
  Upload,
  DataLine,
  Clock,
  User,
  SwitchButton,
  Setting,
  InfoFilled
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { logout } from '@/api/auth'
import LangSwitch from '@/components/LangSwitch.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const currentRoute = computed(() => route.path)
const isAdmin = computed(() => userStore.isAdmin)

// 菜单选择处理
const handleMenuSelect = (index) => {
  router.push(index)
}

// 处理登录
const handleLogin = () => {
  router.push('/login')
}

// 处理登出
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(t('confirmDialog.logout'), t('common.tip'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    })

    await logout()
    userStore.logout()
    router.push('/login')
  } catch {
    // 用户取消操作
  }
}

// 组件挂载时检查用户信息
onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.userInfo?.role) {
    try {
      await userStore.fetchUserInfo()
    } catch (error) {
      console.error('获取用户信息失败:', error)
    }
  }
})
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  background: var(--theme-gradient-bg);
}

.sidebar {
  width: 260px;
  background: linear-gradient(180deg, var(--theme-bg-3) 0%, var(--theme-bg-2) 35%, var(--theme-bg-5) 100%);
  border-right: 1px solid var(--theme-border-2);
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 16px var(--theme-shadow);
  position: relative;
  overflow: hidden;
}

/* 方案2：同色系竖向渐变 + 轻微条纹纹理 */
.sidebar::before {
  content: '';
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    105deg,
    transparent,
    transparent 12px,
    var(--theme-bg-overlay-4) 12px,
    var(--theme-bg-overlay-4) 13.5px
  );
  opacity: 0.7;
  pointer-events: none;
}

.sidebar-header {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  border-bottom: 1px solid var(--theme-border-3);
  background: var(--theme-bg-1);
  position: relative;
  overflow: hidden;
}

.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 1;
  margin-bottom: 12px;
}

.lang-switch {
  position: relative;
  z-index: 1;
}

.logo {
  width: 36px;
  height: 36px;
  object-fit: contain;
  filter: drop-shadow(0 0 8px rgba(var(--theme-shadow-rgb), 0.35));
  position: relative;
  z-index: 1;
}

.title {
  position: relative;
  z-index: 1;
  text-align: center;
}

.title h2 {
  margin: 0;
  font-size: 17px;
  color: var(--theme-text);
  font-weight: 600;
  text-shadow: 0 1px 2px var(--theme-shadow-3);
  white-space: nowrap;
}

.title p {
  margin: 4px 0 0 0;
  font-size: 11px;
  color: var(--theme-text-muted);
  text-shadow: none;
}

.sidebar-menu {
  flex: 1;
  border: none;
  padding: 16px 12px;
  background: transparent;
}

:deep(.el-menu) {
  background: transparent;
}

:deep(.el-menu-item) {
  border-radius: 8px;
  margin-bottom: 4px;
  height: 48px;
  line-height: 48px;
  transition: all 0.3s;
  color: var(--theme-text-soft);
  border: 1px solid transparent;
}

:deep(.el-menu-item:hover) {
  background: var(--theme-bg-overlay-4);
  color: var(--theme-accent);
  border-color: var(--theme-border-4);
  transform: translateX(4px);
  box-shadow: 0 2px 8px var(--theme-shadow-2);
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(var(--theme-primary), 0.9) 0%, rgba(220, 200, 180, 0.95) 100%);
  color: var(--theme-accent);
  border-color: var(--theme-border-6);
  box-shadow: 0 2px 12px var(--theme-shadow-3), inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

:deep(.el-menu-item.is-active .el-icon) {
  color: var(--theme-accent);
  filter: drop-shadow(0 1px 2px var(--theme-shadow-3));
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--theme-border-3);
  background: linear-gradient(180deg, transparent 0%, var(--theme-bg-overlay-2) 100%);
}

.auth-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  transition: all 0.3s;
  border: 1px solid var(--theme-border-5);
}

.auth-button.el-button--primary {
  background: var(--theme-gradient-card);
  color: var(--theme-accent);
  border-color: var(--theme-border-7);
}

.auth-button.el-button--primary:hover {
  background: var(--theme-gradient-button);
  box-shadow: 0 4px 16px var(--theme-shadow-4);
  transform: translateY(-2px);
}

.auth-button.el-button--danger {
  /* 退出按钮默认：中性、与主色协调，不显红 */
  background: var(--theme-gradient-card);
  color: var(--theme-text);
  border-color: var(--theme-border-5);
}

.auth-button.el-button--danger:hover {
  /* 悬停时才变成明显的红色警示 */
  background: var(--theme-btn-danger);
  color: var(--theme-btn-danger-text);
  border-color: var(--theme-btn-danger-hover);
  box-shadow: 0 0 18px rgba(180, 38, 42, 0.45);
  transform: translateY(-2px);
}

.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: var(--theme-gradient-bg);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}
</style>
