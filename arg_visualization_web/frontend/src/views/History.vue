<template>
  <div class="history-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ $t('history.title') }}</span>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              :placeholder="$t('history.searchPlaceholder')"
              style="width: 280px;"
              clearable
              @keyup.enter="handleSearch"
              @clear="refreshTasks"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="handleSearch" :loading="loading">
              {{ $t('common.search') }}
            </el-button>
            <el-button type="primary" link @click="refreshTasks" :loading="loading">
              <el-icon><Refresh /></el-icon>
              {{ $t('upload.refresh') }}
            </el-button>
          </div>
        </div>
      </template>
      
      <el-table :data="paginatedTasks" v-loading="loading">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <el-descriptions :column="2" border>
                <el-descriptions-item :label="$t('history.table.taskId')">{{ row.fileId }}</el-descriptions-item>
                <el-descriptions-item :label="$t('history.table.createdAt')">{{ formatDate(row.createdAt) }}</el-descriptions-item>
                <el-descriptions-item label="Started At">{{ formatDate(row.startedAt) }}</el-descriptions-item>
                <el-descriptions-item :label="$t('history.table.completedAt')">{{ formatDate(row.completedAt) }}</el-descriptions-item>
                <el-descriptions-item label="Duration">{{ formatDuration(row.startedAt, row.completedAt) }}</el-descriptions-item>
                <el-descriptions-item label="Error" v-if="row.errorMessage">
                  <el-text type="danger">{{ row.errorMessage }}</el-text>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column :label="$t('history.table.taskId')" width="100" prop="taskId" />
        <el-table-column prop="fileName" :label="$t('upload.table.filename')" min-width="200" show-overflow-tooltip />
        <el-table-column :label="$t('history.table.status')" width="150">
          <template #default="{ row }">
            <div class="status-cell">
              <el-tag :type="getStatusType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
              <!-- MAG 任务阶段指示 -->
              <span v-if="isMagTask(row) && isProcessing(row.status)" class="stage-indicator">
                {{ getStageText(row.status) }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('history.table.progress')" width="180">
          <template #default="{ row }">
            <div v-if="isProcessing(row.status)" class="progress-cell">
              <el-progress
                :percentage="row.progress || 0"
                :status="row.progress === 100 ? 'success' : ''"
              />
              <!-- MAG 两阶段进度 -->
              <div v-if="isMagTask(row)" class="stage-progress">
                <el-steps :active="getStageNumber(row.status) - 1" simple size="small">
                  <el-step :title="$t('upload.steps.prodigal')" />
                  <el-step :title="$t('upload.steps.arg')" />
                </el-steps>
              </div>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('history.table.createdAt')" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('history.table.actions')" width="240">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'COMPLETED'"
              type="primary"
              link
              size="small"
              @click="handleViewResult(row)"
            >
              {{ $t('history.actions.view') }}
            </el-button>
            <el-button
              v-if="row.status === 'RUNNING'"
              type="warning"
              link
              size="small"
              @click="handleCancelTask(row)"
            >
              {{ $t('history.actions.cancel') }}
            </el-button>
            <el-button
              v-if="row.status === 'FAILED'"
              type="success"
              link
              size="small"
              @click="handleRetryTask(row)"
            >
              Retry
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              @click="handleDeleteTask(row)"
            >
              {{ $t('history.actions.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { getUserTasks, cancelTask, deleteTask, createTask } from '@/api/task'

const { t } = useI18n()
const router = useRouter()

const loading = ref(false)
const tasks = ref([])
const searchKeyword = ref('')
let refreshTimer = null

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

// 计算当前页的数据
const paginatedTasks = computed(() => {
  const start = (pagination.current - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return tasks.value.slice(start, end)
})

// 刷新任务列表
const refreshTasks = async () => {
  try {
    loading.value = true
    const res = await getUserTasks()
    tasks.value = res.data || []
    pagination.total = tasks.value.length
    searchKeyword.value = ''
  } catch (error) {
    console.error('Failed to fetch tasks:', error)
    ElMessage.error(t('history.messages.loadFailed') || 'Failed to load tasks')
    tasks.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// 搜索任务
const handleSearch = async () => {
  if (!searchKeyword.value?.trim()) {
    refreshTasks()
    return
  }
  
  try {
    loading.value = true
    const res = await getUserTasks(searchKeyword.value.trim())
    tasks.value = res.data || []
    pagination.total = tasks.value.length
    pagination.current = 1
    if (tasks.value.length === 0) {
      ElMessage.info(t('history.noTasks'))
    }
  } catch (error) {
    console.error('Failed to search tasks:', error)
    ElMessage.error(t('common.error'))
  } finally {
    loading.value = false
  }
}

// 查看结果
const handleViewResult = (row) => {
  router.push({
    path: '/visualization',
    query: { taskId: row.taskId }
  })
}

// 取消任务
const handleCancelTask = async (row) => {
  try {
    await ElMessageBox.confirm(t('history.confirmCancel'), t('history.confirmCancelTitle'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    })
    
    await cancelTask(row.taskId)
    ElMessage.success(t('history.messages.cancelSuccess'))
    await refreshTasks()
  } catch {
    // 用户取消或操作失败
  }
}

// 重试任务
const handleRetryTask = async (row) => {
  try {
    await ElMessageBox.confirm('Are you sure to retry this task?', t('common.confirm'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'info'
    })
    
    await createTask({ fileId: row.fileId, analysisType: 'arg' })
    ElMessage.success(t('upload.messages.taskCreated'))
    await refreshTasks()
  } catch {
    // 用户取消或操作失败
  }
}

// 删除任务
const handleDeleteTask = async (row) => {
  try {
    await ElMessageBox.confirm(t('history.confirmDelete'), t('history.confirmDeleteTitle'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    })
    
    await deleteTask(row.taskId)
    ElMessage.success(t('history.messages.deleteSuccess'))
    await refreshTasks()
  } catch {
    // 用户取消或操作失败
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString()
}

// 计算运行时长
const formatDuration = (start, end) => {
  if (!start || !end) return '-'
  const startDate = new Date(start)
  const endDate = new Date(end)
  if (isNaN(startDate) || isNaN(endDate)) return '-'

  const diff = endDate.getTime() - startDate.getTime()
  if (diff < 0) return '-'

  const totalSeconds = Math.floor(diff / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  const parts = []
  if (hours) parts.push(`${hours}h`)
  if (minutes) parts.push(`${minutes}m`)
  if (!hours && !minutes) {
    parts.push(`${seconds}s`)
  } else if (seconds) {
    parts.push(`${seconds}s`)
  }

  return parts.join(' ')
}

// 获取状态类型
const getStatusType = (status) => {
  const types = {
    'PENDING': 'info',
    'RUNNING': 'warning',
    'PREPROCESSING': 'warning',
    'ANALYZING': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'CANCELLED': 'info'
  }
  return types[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'PENDING': t('history.status.pending'),
    'RUNNING': t('history.status.running'),
    'PREPROCESSING': t('history.status.running'),
    'ANALYZING': t('history.status.running'),
    'COMPLETED': t('history.status.completed'),
    'FAILED': t('history.status.failed'),
    'CANCELLED': t('history.status.cancelled')
  }
  return statusMap[status] || status
}

// 判断是否为 MAG 任务
const isMagTask = (row) => {
  return row.taskName && row.taskName.includes('MAG')
}

// 判断是否正在处理中
const isProcessing = (status) => {
  return ['RUNNING', 'PREPROCESSING', 'ANALYZING'].includes(status)
}

// 获取阶段编号
const getStageNumber = (status) => {
  if (status === 'PREPROCESSING') return 1
  if (status === 'ANALYZING') return 2
  return 1
}

// 获取阶段文本
const getStageText = (status) => {
  if (status === 'PREPROCESSING') return '(1/2)'
  if (status === 'ANALYZING') return '(2/2)'
  return ''
}

// 分页大小改变
const handleSizeChange = (val) => {
  pagination.pageSize = val
  pagination.current = 1
}

// 当前页改变
const handleCurrentChange = (val) => {
  pagination.current = val
}

onMounted(() => {
  refreshTasks()
  
  // 定时刷新运行中的任务
  refreshTimer = setInterval(() => {
    const hasRunningTask = tasks.value.some(t => t.status === 'RUNNING')
    if (hasRunningTask) {
      refreshTasks()
    }
  }, 5000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.history-container {
  max-width: 1400px;
  margin: 0 auto;
}

:deep(.el-card) {
  background: var(--theme-gradient-card);
  border: 1px solid var(--theme-border-2);
  box-shadow: 0 4px 16px var(--theme-shadow);
}

:deep(.el-card__header) {
  background: var(--theme-bg-overlay-2);
  border-bottom: 1px solid var(--theme-border-3);
  color: var(--theme-accent);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

:deep(.el-table) {
  background: transparent;
  color: var(--theme-text);
}

:deep(.el-table th.el-table__cell) {
  background: var(--theme-bg-overlay-4);
  color: var(--theme-accent);
  border-bottom: 1px solid var(--theme-border-4);
}

:deep(.el-table tr) {
  background: transparent;
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid var(--theme-border);
}

:deep(.el-table__body tr:hover > td) {
  background: var(--theme-bg-overlay-2) !important;
}

:deep(.el-table__expanded-cell) {
  background: var(--theme-bg-overlay);
}

.expand-content {
  padding: 20px 60px;
}

:deep(.el-descriptions) {
  background: transparent;
}

:deep(.el-descriptions__label) {
  color: var(--theme-accent);
  background: var(--theme-bg-overlay-4);
}

:deep(.el-descriptions__content) {
  color: var(--theme-accent);
  background: var(--theme-bg-overlay);
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: var(--theme-accent);
  --el-pagination-button-bg-color: rgba(var(--theme-primary), 0.8);
  --el-pagination-hover-color: var(--theme-accent);
}

:deep(.el-pagination .el-pager li) {
  background: rgba(255, 255, 255, 0.6);
  color: var(--theme-accent);
  border: 1px solid var(--theme-border-3);
}

:deep(.el-pagination .el-pager li.is-active) {
  background: var(--theme-bg-5);
  color: var(--theme-accent);
  border-color: var(--theme-border-7);
}

:deep(.el-pagination .el-pager li:hover) {
  color: var(--theme-accent);
  background: rgba(var(--theme-primary), 0.9);
}

/* 状态标签：成功/失败等统一用实心，成功用紫、失败用深红 */
:deep(.el-tag--success) {
  background: var(--theme-status-success) !important;
  border-color: var(--theme-status-success) !important;
  color: var(--theme-status-success-text) !important;
}
:deep(.el-tag--danger) {
  background: var(--theme-btn-danger) !important;
  border-color: var(--theme-btn-danger) !important;
  color: var(--theme-btn-danger-text) !important;
}
:deep(.el-tag--warning) {
  background: var(--theme-accent-2) !important;
  border-color: var(--theme-accent-2) !important;
  color: #fff !important;
}
:deep(.el-tag--info) {
  background: var(--theme-accent) !important;
  border-color: var(--theme-accent) !important;
  color: #fff !important;
}

/* 操作按钮：主按钮与危险按钮用 theme 色 */
:deep(.el-button--primary) {
  background: var(--theme-btn-primary) !important;
  border-color: var(--theme-btn-primary) !important;
  color: var(--theme-btn-primary-text) !important;
}
:deep(.el-button--primary:hover),
:deep(.el-button--primary:focus) {
  background: var(--theme-btn-primary-hover) !important;
  border-color: var(--theme-btn-primary-hover) !important;
  color: var(--theme-btn-primary-text) !important;
}
:deep(.el-button--primary.is-link) {
  background: transparent !important;
  border-color: transparent !important;
  color: var(--theme-btn-primary) !important;
}
:deep(.el-button--primary.is-link:hover),
:deep(.el-button--primary.is-link:focus) {
  color: var(--theme-btn-primary-hover) !important;
}
:deep(.el-button--danger) {
  color: var(--theme-btn-danger) !important;
  border-color: var(--theme-btn-danger) !important;
}
:deep(.el-button--danger:hover),
:deep(.el-button--danger:focus) {
  color: var(--theme-btn-danger-hover) !important;
  border-color: var(--theme-btn-danger-hover) !important;
}
:deep(.el-button--danger:not(.is-link)) {
  background: var(--theme-btn-danger) !important;
  color: var(--theme-btn-danger-text) !important;
}
:deep(.el-button--danger:not(.is-link):hover),
:deep(.el-button--danger:not(.is-link):focus) {
  background: var(--theme-btn-danger-hover) !important;
  border-color: var(--theme-btn-danger-hover) !important;
  color: var(--theme-btn-danger-text) !important;
}

/* 状态和进度样式 */
.status-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.stage-indicator {
  font-size: 11px;
  color: #E6A23C;
}

.progress-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stage-progress {
  margin-top: 4px;
}

.stage-progress :deep(.el-steps--simple) {
  background: rgba(255, 255, 255, 0.6);
  border-radius: 4px;
  padding: 4px 8px;
}

.stage-progress :deep(.el-step__title) {
  font-size: 11px;
  color: var(--theme-text-light);
}

.stage-progress :deep(.el-step__title.is-process) {
  color: #E6A23C;
  font-weight: 500;
}

.stage-progress :deep(.el-step__title.is-finish) {
  color: #67C23A;
}
</style>
