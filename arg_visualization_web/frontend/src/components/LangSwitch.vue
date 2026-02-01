<template>
  <div class="lang-switch">
    <el-dropdown trigger="click" @command="handleCommand">
      <div class="lang-trigger">
        <svg class="globe-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.5"/>
          <ellipse cx="12" cy="12" rx="4" ry="9" stroke="currentColor" stroke-width="1.5"/>
          <path d="M3 12h18" stroke="currentColor" stroke-width="1.5"/>
          <path d="M12 3c3 2.5 3 16.5 0 18" stroke="currentColor" stroke-width="1.5"/>
          <path d="M12 3c-3 2.5-3 16.5 0 18" stroke="currentColor" stroke-width="1.5"/>
        </svg>
        <span class="lang-text">{{ currentLangLabel }}</span>
        <el-icon class="arrow"><ArrowDown /></el-icon>
      </div>
      <template #dropdown>
        <el-dropdown-menu class="lang-dropdown">
          <el-dropdown-item command="zh" :class="{ active: currentLocale === 'zh' }">
            <span class="lang-option">
              <span class="lang-flag">🇨🇳</span>
              <span class="lang-name">简体中文</span>
              <el-icon v-if="currentLocale === 'zh'" class="check-icon"><Check /></el-icon>
            </span>
          </el-dropdown-item>
          <el-dropdown-item command="en" :class="{ active: currentLocale === 'en' }">
            <span class="lang-option">
              <span class="lang-flag">🇺🇸</span>
              <span class="lang-name">English</span>
              <el-icon v-if="currentLocale === 'en'" class="check-icon"><Check /></el-icon>
            </span>
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ArrowDown, Check } from '@element-plus/icons-vue'
import { currentLocale, setLocale } from '@/locales'

const currentLangLabel = computed(() => {
  return currentLocale.value === 'zh' ? '中文' : 'EN'
})

const handleCommand = (lang) => {
  setLocale(lang)
}
</script>

<style scoped>
.lang-switch {
  cursor: pointer;
}

.lang-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.lang-trigger:hover {
  background: rgba(0, 255, 255, 0.1);
  border-color: rgba(0, 255, 255, 0.3);
  color: #00ffff;
}

.globe-icon {
  width: 20px;
  height: 20px;
  color: currentColor;
  transition: all 0.3s ease;
}

.lang-trigger:hover .globe-icon {
  filter: drop-shadow(0 0 4px rgba(0, 255, 255, 0.5));
  transform: rotate(15deg);
}

.lang-text {
  font-size: 14px;
  font-weight: 500;
  min-width: 28px;
}

.arrow {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  transition: transform 0.3s;
}

.lang-trigger:hover .arrow {
  color: #00ffff;
}

/* Dropdown 样式 */
:deep(.lang-dropdown) {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  border: 1px solid rgba(0, 255, 255, 0.2);
  border-radius: 10px;
  padding: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4), 0 0 20px rgba(0, 255, 255, 0.1);
  min-width: 160px;
}

:deep(.el-dropdown-menu__item) {
  border-radius: 8px;
  padding: 12px 16px;
  color: rgba(255, 255, 255, 0.8);
  margin: 2px 0;
  transition: all 0.3s ease;
}

:deep(.el-dropdown-menu__item:hover) {
  background: rgba(0, 255, 255, 0.1);
  color: #00ffff;
}

:deep(.el-dropdown-menu__item.active) {
  background: rgba(0, 255, 255, 0.15);
  color: #00ffff;
}

.lang-option {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.lang-flag {
  font-size: 20px;
}

.lang-name {
  flex: 1;
  font-size: 14px;
}

.check-icon {
  color: #00ffff;
  font-size: 16px;
}
</style>
