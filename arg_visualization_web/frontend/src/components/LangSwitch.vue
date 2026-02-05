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
  color: var(--theme-text-soft);
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.lang-trigger:hover {
  background: var(--theme-bg-overlay-4);
  border-color: var(--theme-border-4);
  color: var(--theme-accent);
}

.globe-icon {
  width: 20px;
  height: 20px;
  color: currentColor;
  transition: all 0.3s ease;
}

.lang-trigger:hover .globe-icon {
  filter: drop-shadow(0 1px 3px var(--theme-shadow-3));
  transform: rotate(15deg);
}

.lang-text {
  font-size: 14px;
  font-weight: 500;
  min-width: 28px;
}

.arrow {
  font-size: 12px;
  color: var(--theme-text-faint);
  transition: transform 0.3s;
}

.lang-trigger:hover .arrow {
  color: var(--theme-accent);
}

/* Dropdown 样式 */
:deep(.lang-dropdown) {
  background: var(--theme-gradient-bg);
  border: 1px solid var(--theme-border-3);
  border-radius: 10px;
  padding: 8px;
  box-shadow: 0 8px 24px var(--theme-shadow-3), 0 0 20px var(--theme-bg-overlay-3);
  min-width: 160px;
}

:deep(.el-dropdown-menu__item) {
  border-radius: 8px;
  padding: 12px 16px;
  color: var(--theme-text-soft);
  margin: 2px 0;
  transition: all 0.3s ease;
}

:deep(.el-dropdown-menu__item:hover) {
  background: var(--theme-bg-overlay-4);
  color: var(--theme-accent);
}

:deep(.el-dropdown-menu__item.active) {
  background: var(--theme-bg-overlay-5);
  color: var(--theme-accent);
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
  color: var(--theme-accent);
  font-size: 16px;
}
</style>
