<template>
  <div class="admin-container">
    <!-- 系统统计 -->
    <el-card class="stats-card">
      <template #header>
        <div class="card-header">
          <el-icon><DataAnalysis /></el-icon>
          <span>{{ $t('admin.systemStats') }}</span>
        </div>
      </template>
      
      <el-row :gutter="24">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-icon" style="color: #409eff;">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ statistics.totalUsers || 0 }}</div>
              <div class="stat-label">{{ $t('admin.stats.totalUsers') }}</div>
            </div>
          </div>
        </el-col>
        
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-icon" style="color: var(--theme-status-success);">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ statistics.totalFiles || 0 }}</div>
              <div class="stat-label">{{ $t('admin.stats.totalFiles') }}</div>
            </div>
          </div>
        </el-col>
        
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-icon" style="color: var(--theme-stat-tasks);">
              <el-icon><List /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ statistics.totalTasks || 0 }}</div>
              <div class="stat-label">{{ $t('admin.stats.totalTasks') }}</div>
            </div>
          </div>
        </el-col>
        
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-icon" style="color: #f56c6c;">
              <el-icon><Key /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ statistics.totalLogins || 0 }}</div>
              <div class="stat-label">{{ $t('admin.stats.totalLogins') }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- Tab切换 -->
    <el-card class="main-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 用户管理 -->
        <el-tab-pane :label="$t('admin.tabs.users')" name="users">
          <div class="tab-header">
            <el-input
              v-model="searchKeyword.users"
              :placeholder="$t('admin.users.searchPlaceholder')"
              style="width: 300px; margin-right: 10px;"
              clearable
              @keyup.enter="handleSearchUsers"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="handleSearchUsers" :loading="loading.users">
              <el-icon><Search /></el-icon>
              {{ $t('common.search') }}
            </el-button>
            <el-button type="primary" @click="fetchUsers" :loading="loading.users">
              <el-icon><Refresh /></el-icon>
              {{ $t('common.reset') }}
            </el-button>
          </div>
          
          <el-table
            :data="userList"
            v-loading="loading.users"
            stripe
            border
            style="width: 100%"
            class="data-table"
          >
            <el-table-column prop="userId" :label="$t('admin.users.table.userId')" width="80" />
            <el-table-column prop="username" :label="$t('admin.users.table.username')" width="150" />
            <el-table-column prop="email" :label="$t('admin.users.table.email')" width="200" />
            <el-table-column prop="role" :label="$t('admin.users.table.role')" width="100">
              <template #default="{ row }">
                <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'">
                  {{ row.role === 'ADMIN' ? $t('admin.users.roles.admin') : $t('admin.users.roles.user') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="$t('admin.users.table.status')" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
                  {{ row.status === 'ACTIVE' ? $t('admin.users.status.active') : $t('admin.users.status.banned') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="fileCount" :label="$t('admin.users.table.fileCount')" width="100" />
            <el-table-column prop="taskCount" :label="$t('admin.users.table.taskCount')" width="100" />
            <el-table-column prop="createdAt" :label="$t('admin.users.table.createdAt')" width="180" />
            <el-table-column :label="$t('admin.users.table.lastLogin')" width="240">
              <template #default="{ row }">
                <div v-if="row.lastLoginAt">
                  <div>{{ row.lastLoginAt }}</div>
                  <div v-if="row.lastLoginLocation" style="color: #909399; font-size: 12px; margin-top: 4px;">
                    {{ row.lastLoginLocation?.[currentLocale] ?? row.lastLoginLocation?.zh ?? row.lastLoginLocation?.en ?? '' }}
                  </div>
                </div>
                <span v-else style="color: #909399;">{{ $t('admin.users.notLoggedIn') }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="$t('admin.users.table.actions')" width="120">
              <template #default="{ row }">
                <el-button
                  v-if="row.role !== 'ADMIN'"
                  type="danger"
                  size="small"
                  @click="handleDeleteUser(row)"
                >
                  {{ $t('admin.users.actions.delete') }}
                </el-button>
                <el-tag v-else type="info" size="small">{{ $t('admin.users.roles.admin') }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="userPagination.pageNum"
              v-model:page-size="userPagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="userPagination.total"
              layout="total, sizes, prev, pager, next"
              @current-change="onUserPageChange"
              @size-change="onUserSizeChange"
            />
          </div>
        </el-tab-pane>

        <!-- 文件管理 -->
        <el-tab-pane :label="$t('admin.tabs.files')" name="files">
          <div class="tab-header">
            <el-input
              v-model="searchKeyword.files.user"
              :placeholder="$t('admin.files.searchUser')"
              style="width: 200px; margin-right: 10px;"
              clearable
              @keyup.enter="handleSearchFiles"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
            <el-input
              v-model="searchKeyword.files.file"
              :placeholder="$t('admin.files.searchFile')"
              style="width: 250px; margin-right: 10px;"
              clearable
              @keyup.enter="handleSearchFiles"
            >
              <template #prefix>
                <el-icon><Document /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="handleSearchFiles" :loading="loading.files">
              <el-icon><Search /></el-icon>
              {{ $t('common.search') }}
            </el-button>
            <el-button type="primary" @click="fetchFiles" :loading="loading.files">
              <el-icon><Refresh /></el-icon>
              {{ $t('common.reset') }}
            </el-button>
          </div>
          
          <el-table
            :data="fileList"
            v-loading="loading.files"
            stripe
            border
            style="width: 100%"
            class="data-table"
          >
            <el-table-column prop="fileId" :label="$t('admin.files.table.fileId')" width="80" />
            <el-table-column prop="userId" :label="$t('admin.files.table.userId')" width="80" />
            <el-table-column prop="username" :label="$t('admin.files.table.username')" width="150" />
            <el-table-column prop="originalFilename" :label="$t('admin.files.table.filename')" min-width="200" show-overflow-tooltip />
            <el-table-column prop="fileSize" :label="$t('admin.files.table.fileSize')" width="120">
              <template #default="{ row }">
                {{ formatFileSize(row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column prop="fileType" :label="$t('admin.files.table.fileType')" width="100" />
            <el-table-column prop="status" :label="$t('admin.files.table.status')" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="uploadTime" :label="$t('admin.files.table.uploadTime')" width="180" />
            <el-table-column :label="$t('admin.files.table.actions')" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="danger"
                  size="small"
                  @click="handleDeleteFile(row)"
                >
                  {{ $t('admin.users.actions.delete') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="filePagination.pageNum"
              v-model:page-size="filePagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="filePagination.total"
              layout="total, sizes, prev, pager, next"
              @current-change="onFilePageChange"
              @size-change="onFileSizeChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useI18n } from 'vue-i18n';
import {
  DataAnalysis,
  User,
  Document,
  List,
  Key,
  Refresh,
  Search,
} from '@element-plus/icons-vue';
import {
  getUsersPage,
  deleteUser,
  getFilesPage,
  deleteFile,
  getStatistics,
} from '@/api/admin';
import { currentLocale } from '@/locales';

const { t } = useI18n();

const activeTab = ref('users');
const loading = ref({
  users: false,
  files: false,
});

const statistics = ref({
  totalUsers: 0,
  totalFiles: 0,
  totalTasks: 0,
  totalLogins: 0,
});

const userList = ref([]);
const fileList = ref([]);

// 用户分页
const userPagination = ref({
  pageNum: 1,
  pageSize: 10,
  total: 0,
});
// 文件分页
const filePagination = ref({
  pageNum: 1,
  pageSize: 10,
  total: 0,
});

const searchKeyword = ref({
  users: '',
  files: {
    user: '',
    file: '',
  },
});

// 获取统计信息
const fetchStatistics = async () => {
  try {
    const res = await getStatistics();
    statistics.value = res.data;
  } catch (error) {
    console.error('获取统计信息失败:', error);
  }
};

// 获取用户列表（分页，可选关键字）
const fetchUsers = async (resetPage = true) => {
  if (resetPage) userPagination.value.pageNum = 1;
  loading.value.users = true;
  try {
    const params = {
      pageNum: userPagination.value.pageNum,
      pageSize: userPagination.value.pageSize,
    };
    if (searchKeyword.value.users?.trim()) {
      params.keyword = searchKeyword.value.users.trim();
    }
    const res = await getUsersPage(params);
    userList.value = res.data?.list ?? [];
    userPagination.value.total = res.data?.total ?? 0;
    if (resetPage) searchKeyword.value.users = '';
  } catch (error) {
    ElMessage.error(t('admin.messages.getUsersFailed') + ': ' + (error.message || ''));
  } finally {
    loading.value.users = false;
  }
};

// 搜索用户（第 1 页）
const handleSearchUsers = async () => {
  userPagination.value.pageNum = 1;
  await fetchUsers(false);
  if (userList.value.length === 0 && searchKeyword.value.users?.trim()) {
    ElMessage.info(t('admin.users.noMatch'));
  }
};

// 用户分页变更
const onUserPageChange = (page) => {
  userPagination.value.pageNum = page;
  fetchUsers(false);
};
const onUserSizeChange = (size) => {
  userPagination.value.pageSize = size;
  userPagination.value.pageNum = 1;
  fetchUsers(false);
};

// 获取文件列表（分页，可选关键字）
const fetchFiles = async (resetPage = true) => {
  if (resetPage) {
    filePagination.value.pageNum = 1;
    searchKeyword.value.files.user = '';
    searchKeyword.value.files.file = '';
  }
  loading.value.files = true;
  try {
    const params = {
      pageNum: filePagination.value.pageNum,
      pageSize: filePagination.value.pageSize,
    };
    if (searchKeyword.value.files.user?.trim()) {
      params.userKeyword = searchKeyword.value.files.user.trim();
    }
    if (searchKeyword.value.files.file?.trim()) {
      params.fileKeyword = searchKeyword.value.files.file.trim();
    }
    const res = await getFilesPage(params);
    fileList.value = res.data?.list ?? [];
    filePagination.value.total = res.data?.total ?? 0;
  } catch (error) {
    ElMessage.error(t('admin.messages.getFilesFailed') + ': ' + (error.message || ''));
  } finally {
    loading.value.files = false;
  }
};

// 搜索文件（第 1 页）
const handleSearchFiles = async () => {
  filePagination.value.pageNum = 1;
  await fetchFiles(false);
  if (fileList.value.length === 0 && (searchKeyword.value.files.user?.trim() || searchKeyword.value.files.file?.trim())) {
    ElMessage.info(t('admin.files.noMatch'));
  }
};

// 文件分页变更
const onFilePageChange = (page) => {
  filePagination.value.pageNum = page;
  fetchFiles(false);
};
const onFileSizeChange = (size) => {
  filePagination.value.pageSize = size;
  filePagination.value.pageNum = 1;
  fetchFiles(false);
};

// 删除用户（乐观更新：先从列表移除，后台请求完成后失败则恢复）
const handleDeleteUser = async (row) => {
  try {
    await ElMessageBox.confirm(
      t('admin.users.confirmDelete', { name: row.username }),
      t('admin.dangerAction'),
      {
        confirmButtonText: t('admin.confirmDelete'),
        cancelButtonText: t('common.cancel'),
        type: 'error',
        distinguishCancelAndClose: true,
      }
    );

    const userId = row.userId;
    const idx = userList.value.findIndex((u) => u.userId === userId);
    const backup = idx >= 0 ? userList.value[idx] : null;
    if (idx >= 0) {
      userList.value = userList.value.filter((u) => u.userId !== userId);
    }
    const loadingMsg = ElMessage({ type: 'info', message: t('admin.users.deleting') || '正在删除…', duration: 0 });
    try {
      await deleteUser(userId);
      loadingMsg.close();
      ElMessage.success(t('admin.users.deleteSuccess'));
      fetchStatistics();
      fetchUsers(false);
    } catch (err) {
      loadingMsg.close();
      if (backup) userList.value = [...userList.value.slice(0, idx), backup, ...userList.value.slice(idx)];
      ElMessage.error(t('admin.messages.deleteUserFailed') + ': ' + (err.message || ''));
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(t('admin.messages.deleteUserFailed') + ': ' + (error.message || ''));
    }
  }
};

// 删除文件（乐观更新：先从列表移除，后台请求完成后失败则恢复）
const handleDeleteFile = async (row) => {
  try {
    await ElMessageBox.confirm(
      t('admin.files.confirmDelete', { name: row.originalFilename }),
      t('admin.dangerAction'),
      {
        confirmButtonText: t('admin.confirmDelete'),
        cancelButtonText: t('common.cancel'),
        type: 'error',
        distinguishCancelAndClose: true,
      }
    );

    const fileId = row.fileId;
    const idx = fileList.value.findIndex((f) => f.fileId === fileId);
    const backup = idx >= 0 ? fileList.value[idx] : null;
    if (idx >= 0) {
      fileList.value = fileList.value.filter((f) => f.fileId !== fileId);
    }
    const loadingMsg = ElMessage({ type: 'info', message: t('admin.files.deleting') || '正在删除…', duration: 0 });
    try {
      await deleteFile(fileId);
      loadingMsg.close();
      ElMessage.success(t('admin.files.deleteSuccess'));
      fetchStatistics();
      fetchFiles(false);
    } catch (err) {
      loadingMsg.close();
      if (backup) fileList.value = [...fileList.value.slice(0, idx), backup, ...fileList.value.slice(idx)];
      ElMessage.error(t('admin.messages.deleteFileFailed') + ': ' + (err.message || ''));
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(t('admin.messages.deleteFileFailed') + ': ' + (error.message || ''));
    }
  }
};

// Tab切换处理
const handleTabChange = (tabName) => {
  if (tabName === 'users' && userList.value.length === 0) {
    fetchUsers();
  } else if (tabName === 'files' && fileList.value.length === 0) {
    fetchFiles();
  }
};

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i];
};

// 获取状态类型
const getStatusType = (status) => {
  const types = {
    UPLOADED: 'success',
    ANALYZING: 'warning',
    COMPLETED: 'success',
    DELETED: 'info',
    FAILED: 'danger',
  };
  return types[status] || 'info';
};

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    UPLOADED: 'admin.files.status.uploaded',
    ANALYZING: 'admin.files.status.analyzing',
    COMPLETED: 'admin.files.status.completed',
    DELETED: 'admin.files.status.deleted',
    FAILED: 'admin.files.status.failed',
  };
  return statusMap[status] ? t(statusMap[status]) : status;
};

onMounted(() => {
  fetchStatistics();
  fetchUsers();
});
</script>

<style scoped>
.admin-container {
  max-width: 1600px;
  margin: 0 auto;
}

.stats-card {
  margin-bottom: 24px;
  background: var(--theme-gradient-bg);
  border: 1px solid var(--theme-border-2);
  box-shadow: 0 8px 32px var(--theme-shadow-2);
}

:deep(.stats-card .el-card__body) {
  background: transparent;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--theme-accent);
  text-shadow: 0 1px 2px rgba(92, 64, 51, 0.2);
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 20px;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  border: 1px solid var(--theme-border-2);
  transition: all 0.3s;
}

.stat-item:hover {
  background: rgba(240, 220, 202, 0.9);
  border-color: var(--theme-border-5);
  transform: translateY(-4px);
  box-shadow: 0 8px 24px var(--theme-shadow-2);
}

.stat-icon {
  font-size: 48px;
  margin-right: 16px;
  filter: drop-shadow(0 0 10px currentColor);
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--theme-accent);
  text-shadow: 0 1px 2px rgba(92, 64, 51, 0.2);
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: var(--theme-text-light);
  font-weight: 500;
}

.main-card {
  background: var(--theme-gradient-card);
  border: 1px solid var(--theme-border-2);
  box-shadow: 0 8px 32px rgba(92, 64, 51, 0.12);
}

:deep(.main-card .el-card__body) {
  background: transparent;
  padding: 0;
}

:deep(.el-tabs__header) {
  margin: 0;
  padding: 20px 20px 0;
  background: transparent;
}

:deep(.el-tabs__nav-wrap::after) {
  background: var(--theme-border-3);
}

:deep(.el-tabs__item) {
  color: rgba(92, 64, 51, 0.7);
  font-size: 16px;
  font-weight: 500;
}

:deep(.el-tabs__item:hover) {
  color: var(--theme-accent);
}

:deep(.el-tabs__item.is-active) {
  color: var(--theme-accent);
  text-shadow: 0 1px 2px rgba(92, 64, 51, 0.2);
}

:deep(.el-tabs__active-bar) {
  background: var(--theme-accent);
  box-shadow: 0 1px 4px rgba(92, 64, 51, 0.3);
}

:deep(.el-tabs__content) {
  padding: 20px;
}

.tab-header {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

.data-table {
  background: transparent !important;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 强制覆盖所有可能的白色背景 */
:deep(.el-table__body),
:deep(.el-table__header),
:deep(.el-table__header th),
:deep(.el-table__body td),
:deep(.el-table__row),
:deep(.el-table__cell),
:deep(.el-table__body tr),
:deep(.el-table__body tr td),
:deep(.el-table__header tr),
:deep(.el-table__header tr th) {
  background-color: transparent !important;
  background-image: none !important;
}

/* 确保所有td和th都有米色背景 */
:deep(.el-table td.el-table__cell),
:deep(.el-table th.el-table__cell) {
  background: rgba(255, 255, 255, 0.4) !important;
}

/* 确保表格容器也是米色 */
:deep(.el-table__inner-wrapper) {
  background: rgba(240, 220, 202, 0.3) !important;
}

:deep(.el-table) {
  background: rgba(240, 220, 202, 0.2) !important;
  color: var(--theme-accent) !important;
}

:deep(.el-table th.el-table__cell) {
  background: rgba(120, 90, 70, 0.12) !important;
  color: var(--theme-accent) !important;
  border-color: var(--theme-border-4) !important;
  font-weight: 600;
}

:deep(.el-table tr) {
  background: rgba(255, 255, 255, 0.3) !important;
}

:deep(.el-table td.el-table__cell) {
  background: rgba(255, 255, 255, 0.35) !important;
  border-color: var(--theme-border-2) !important;
  color: var(--theme-accent) !important;
}

:deep(.el-table__body-wrapper) {
  background: transparent !important;
}

:deep(.el-table__header-wrapper) {
  background: transparent !important;
}

:deep(.el-table__body tr:hover > td) {
  background: rgba(240, 220, 202, 0.6) !important;
}

:deep(.el-table__row--striped td) {
  background: rgba(228, 208, 188, 0.5) !important;
}

:deep(.el-table__body tr.el-table__row--striped:hover > td) {
  background: rgba(240, 220, 202, 0.7) !important;
}

/* 表格边框颜色 */
:deep(.el-table--border) {
  border-color: var(--theme-border-3) !important;
}

:deep(.el-table--border::after) {
  background-color: var(--theme-border-3) !important;
}

:deep(.el-table--border::before) {
  background-color: var(--theme-border-3) !important;
}

:deep(.el-table__inner-wrapper::before) {
  background-color: var(--theme-border-3) !important;
}

:deep(.el-table__inner-wrapper::after) {
  background-color: var(--theme-border-3) !important;
}

/* 标签背景色 - 米色系 */
:deep(.el-tag) {
  background: rgba(240, 220, 202, 0.9) !important;
  border-color: var(--theme-border-6) !important;
  color: var(--theme-accent) !important;
}

:deep(.el-tag.el-tag--primary) {
  background: rgb(222, 202, 182) !important;
  border-color: var(--theme-border-7) !important;
  color: var(--theme-accent) !important;
}

:deep(.el-tag.el-tag--success) {
  background: var(--theme-status-success) !important;
  border-color: var(--theme-status-success) !important;
  color: var(--theme-status-success-text) !important;
}

:deep(.el-tag.el-tag--warning) {
  background: linear-gradient(135deg, rgba(255, 200, 0, 0.2) 0%, rgba(255, 150, 0, 0.25) 100%) !important;
  border-color: rgba(255, 200, 0, 0.4) !important;
  color: #ffc800 !important;
}

:deep(.el-tag.el-tag--danger) {
  background: var(--theme-btn-danger) !important;
  border-color: var(--theme-btn-danger) !important;
  color: var(--theme-btn-danger-text) !important;
}

:deep(.el-tag.el-tag--info) {
  background: rgba(228, 208, 188, 0.9) !important;
  border-color: var(--theme-border-5) !important;
  color: var(--theme-accent) !important;
}

/* Loading遮罩层背景 */
:deep(.el-loading-mask) {
  background-color: rgba(var(--theme-primary), 0.85) !important;
}

/* 空状态背景 */
:deep(.el-empty) {
  background: transparent;
}

:deep(.el-empty__image) {
  opacity: 0.6;
}

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

:deep(.el-button--danger) {
  background: var(--theme-btn-danger) !important;
  border-color: var(--theme-btn-danger) !important;
  color: var(--theme-btn-danger-text) !important;
}

:deep(.el-button--danger:hover),
:deep(.el-button--danger:focus) {
  background: var(--theme-btn-danger-hover) !important;
  border-color: var(--theme-btn-danger-hover) !important;
  color: var(--theme-btn-danger-text) !important;
}

:deep(.el-button--warning) {
  background: linear-gradient(135deg, rgba(255, 200, 0, 0.3) 0%, rgba(255, 150, 0, 0.3) 100%);
  border: 1px solid rgba(255, 200, 0, 0.5);
  color: #ffc800;
}

:deep(.el-button--warning:hover) {
  background: linear-gradient(135deg, rgba(255, 200, 0, 0.4) 0%, rgba(255, 150, 0, 0.4) 100%);
  box-shadow: 0 0 15px rgba(255, 200, 0, 0.4);
}

:deep(.el-button--success) {
  background: linear-gradient(135deg, rgba(0, 255, 150, 0.3) 0%, rgba(0, 200, 100, 0.3) 100%);
  border: 1px solid rgba(0, 255, 150, 0.5);
  color: #00ff99;
}

:deep(.el-button--success:hover) {
  background: linear-gradient(135deg, rgba(0, 255, 150, 0.4) 0%, rgba(0, 200, 100, 0.4) 100%);
  box-shadow: 0 0 15px rgba(0, 255, 150, 0.4);
}
</style>

