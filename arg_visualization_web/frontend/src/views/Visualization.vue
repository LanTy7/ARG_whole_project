<template>
  <div class="visualization-container">
    <el-card v-if="!taskId || !argData" class="empty-state-card">
      <el-empty :description="$t('visualization.emptyDescription')">
        <el-button type="primary" class="empty-state-primary-btn" @click="router.push('/history')">
          {{ $t('home.viewHistory') }}
        </el-button>
      </el-empty>
    </el-card>
    
    <template v-else>
      <!-- 顶部信息栏 -->
      <el-card class="info-header">
        <el-descriptions :column="3" border>
          <el-descriptions-item :label="$t('visualization.info.taskId')">{{ taskId }}</el-descriptions-item>
          <el-descriptions-item :label="$t('visualization.info.taskName')">{{ argData.genomeInfo?.taskName || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('visualization.info.analysisType')">
            <el-tag type="warning">{{ $t('visualization.info.argDetection') }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('visualization.info.resultSummary')" :span="3">
            <el-tag type="success" size="large">
              <el-icon><Document /></el-icon>
              {{ $t('visualization.info.resultCount', { total: totalCount, arg: argPositiveCount }) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
      
      <!-- 标签页 -->
      <el-card class="tabs-card">
        <el-tabs v-model="activeTab" @tab-click="handleTabClick">
          <!-- ARG 预测结果详情 -->
          <el-tab-pane :label="$t('visualization.tab.detail')" name="detail">
            <div class="detail-content" v-loading="loading">
              <div class="detail-header">
                <div>
                  <h3>{{ $t('visualization.detail.title') }}</h3>
                  <p class="summary-desc">
                    {{ $t('visualization.detail.summaryDesc', { total: totalCount, arg: argPositiveCount, nonArg: argNegativeCount }) }}
                  </p>
                </div>
                <el-button type="primary" :icon="Download" @click="downloadArgResults">
                  {{ $t('visualization.detail.downloadArg') }}
                </el-button>
              </div>
              
              <!-- 颜色图例 -->
              <el-alert 
                v-if="totalCount > 0"
                type="info" 
                :closable="false" 
                style="margin-bottom: 16px;"
              >
                <template #title>
                  <div class="legend-container">
                    <span style="font-weight: bold; margin-right: 20px;">{{ $t('visualization.detail.legendTitle') }}</span>
                    <span class="legend-item">
                      <span class="legend-box arg-positive-box"></span>
                      {{ $t('visualization.detail.legendArg') }}
                    </span>
                    <span class="legend-item">
                      <span class="legend-box arg-negative-box"></span>
                      {{ $t('visualization.detail.legendNonArg') }}
                    </span>
                  </div>
                </template>
              </el-alert>
              
              <el-empty v-if="totalCount === 0" :description="$t('visualization.detail.noResults')" />
              
              <template v-else>
                <!-- 筛选和搜索 -->
                <div class="table-toolbar">
                  <el-input
                    v-model="searchKeyword"
                    :placeholder="$t('visualization.detail.searchPlaceholder')"
                    clearable
                    style="width: 300px;"
                    @input="handleSearch"
                  >
                    <template #prefix>
                      <el-icon><Search /></el-icon>
                    </template>
                  </el-input>
                  <el-select v-model="filterArgType" :placeholder="$t('visualization.detail.filterType')" style="width: 150px;" @change="handleFilter">
                    <el-option :label="$t('visualization.filterAll')" value="all" />
                    <el-option :label="$t('visualization.filterArgOnly')" value="arg" />
                    <el-option :label="$t('visualization.filterNonArgOnly')" value="non-arg" />
                  </el-select>
                  <span class="filter-info">
                    {{ $t('visualization.detail.showCount', { filtered: tableTotal, total: totalCount }) }}
                  </span>
                </div>
                
                <el-table 
                  :data="paginatedResults" 
                  border
                  style="margin-top: 16px;"
                  :row-class-name="getArgRowClassName"
                  max-height="500"
                >
                  <el-table-column type="index" :label="$t('common.index') || '#'" width="70" align="center">
                    <template #default="scope">
                      {{ (pagination.currentPage - 1) * pagination.pageSize + scope.$index + 1 }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="id" :label="$t('visualization.table.sequenceId')" min-width="300" show-overflow-tooltip />
                  <el-table-column :label="$t('visualization.table.isArg')" width="120" align="center">
                    <template #default="{ row }">
                      <el-tag :type="row.isArg ? 'success' : 'danger'" size="small">
                        {{ row.isArg ? $t('visualization.yes') : $t('visualization.no') }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column :label="$t('visualization.table.binaryProb')" width="120" align="center">
                    <template #default="{ row }">
                      <span v-if="row.predProb !== null && row.predProb !== undefined">
                        {{ (row.predProb * 100).toFixed(2) }}%
                      </span>
                      <span v-else style="color: #999;">-</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="argClass" :label="$t('visualization.table.argClass')" width="180" align="center">
                    <template #default="{ row }">
                      <!-- 有 Top-5 数据时显示悬停弹窗 -->
                      <el-popover
                        v-if="row.argClass && row.topClasses && row.topClasses.length > 0"
                        placement="top"
                        :width="300"
                        trigger="hover"
                        popper-class="top-classes-popover"
                      >
                        <template #reference>
                          <el-tag type="info" size="small" class="arg-class-tag">
                            {{ row.argClass }}
                            <el-icon class="info-icon"><InfoFilled /></el-icon>
                          </el-tag>
                        </template>
                        
                        <!-- 弹窗内容：Top-5 分类 -->
                        <div class="top-classes-content">
                          <div class="popover-title">{{ $t('visualization.topClasses') }}</div>
                          <div 
                            v-for="(item, idx) in row.topClasses" 
                            :key="idx"
                            :class="['top-class-item', { 'top-class-first': idx === 0 }]"
                          >
                            <span class="rank">{{ idx + 1 }}.</span>
                            <span class="class-name">{{ item.class }}</span>
                            <el-progress 
                              :percentage="item.prob * 100" 
                              :stroke-width="10"
                              :show-text="false"
                              :color="getProgressColor(item.prob * 100)"
                              style="width: 80px; margin: 0 8px;"
                            />
                            <span class="prob-value">{{ (item.prob * 100).toFixed(1) }}%</span>
                          </div>
                        </div>
                      </el-popover>
                      
                      <!-- 没有 Top-5 数据时普通显示 -->
                      <el-tag v-else-if="row.argClass" type="info" size="small">
                        {{ row.argClass }}
                      </el-tag>
                      <span v-else style="color: #999;">-</span>
                    </template>
                  </el-table-column>
                  <el-table-column :label="$t('visualization.table.classProb')" width="120" align="center">
                    <template #default="{ row }">
                      <span v-if="row.classProb !== null && row.classProb !== undefined">
                        {{ (row.classProb * 100).toFixed(2) }}%
                      </span>
                      <span v-else style="color: #999;">-</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="BLAST" width="100" align="center" fixed="right">
                    <template #default="{ row }">
                      <el-button
                        v-if="row.isArg"
                        type="primary"
                        size="small"
                        :icon="Search"
                        @click="handleBlast(row)"
                        class="blast-btn"
                      >
                        BLAST
                      </el-button>
                      <span v-else style="color: #ccc;">-</span>
                    </template>
                  </el-table-column>
                </el-table>
                
                <!-- 分页 -->
                <div class="pagination-wrapper">
                  <el-pagination
                    v-model:current-page="pagination.currentPage"
                    v-model:page-size="pagination.pageSize"
                    :page-sizes="[50, 100, 200, 500]"
                    :total="tableTotal"
                    layout="total, sizes, prev, pager, next, jumper"
                    @size-change="handlePageSizeChange"
                    @current-change="handlePageChange"
                  />
                </div>
              </template>
            </div>
          </el-tab-pane>
          
          <!-- 可视化图表标签页 -->
          <el-tab-pane :label="$t('visualization.tab.charts')" name="charts">
            <div class="charts-content" v-loading="loading">
              <div class="charts-header">
                <h3>{{ $t('visualization.chartsPage.title') }}</h3>
                <el-button type="primary" :icon="Download" @click="downloadChartImages">
                  {{ $t('visualization.chartsPage.downloadCharts') }}
                </el-button>
              </div>
              
              <el-empty v-if="totalCount === 0" :description="$t('visualization.chartsPage.noData')" />
              
              <div v-else class="charts-grid">
                <div class="chart-container">
                  <h4>{{ $t('visualization.chartsPage.pieTitle') }}</h4>
                  <p class="chart-desc">{{ $t('visualization.chartsPage.pieDesc') }}</p>
                  <div ref="pieChartRef" class="chart" style="height: 400px;"></div>
                </div>
                
                <div class="chart-container">
                  <h4>{{ $t('visualization.chartsPage.barTitle') }}</h4>
                  <p class="chart-desc">{{ $t('visualization.chartsPage.barDesc') }}</p>
                  <div ref="barChartRef" class="chart" style="height: 400px;"></div>
                  <el-empty v-if="argClassStats.length === 0 && argPositiveCount > 0" :description="$t('visualization.chartsPage.noCategory')" />
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
      
      <!-- 下载结果区域 -->
      <el-card class="download-card">
        <template #header>
          <div class="card-header">
            <span><el-icon><Download /></el-icon> {{ $t('visualization.downloadCard.title') }}</span>
          </div>
        </template>
        
        <div class="download-content" v-loading="downloadLoading">
          <el-alert 
            v-if="isMagTask" 
            type="info" 
            :closable="false" 
            style="margin-bottom: 16px;"
          >
            <template #title>
              <span>{{ $t('visualization.downloadCard.magTip') }}</span>
            </template>
          </el-alert>
          
          <div class="download-items">
            <div class="download-item">
              <div class="download-info">
                <el-icon class="file-icon"><Document /></el-icon>
                <div class="file-details">
                  <span class="file-name">{{ $t('visualization.downloadCard.argFileName') }}</span>
                  <span class="file-desc">{{ $t('visualization.downloadCard.argFileDesc') }}</span>
                  <span class="file-size" v-if="downloadFiles.arg">{{ downloadFiles.arg.sizeFormatted }}</span>
                </div>
              </div>
              <el-button type="primary" size="small" @click="handleDownload('arg')" :loading="downloading.arg">
                <el-icon><Download /></el-icon> {{ $t('visualization.downloadCard.downloadBtn') }}
              </el-button>
            </div>
            
            <template v-if="isMagTask">
              <div class="download-item" v-if="downloadFiles.merged">
                <div class="download-info">
                  <el-icon class="file-icon" style="color: #67C23A;"><Document /></el-icon>
                  <div class="file-details">
                    <span class="file-name">{{ $t('visualization.downloadCard.mergedFileName') }}</span>
                    <span class="file-desc">{{ $t('visualization.downloadCard.mergedFileDesc') }}</span>
                    <span class="file-size">{{ downloadFiles.merged.sizeFormatted }}</span>
                  </div>
                </div>
                <el-button type="success" size="small" @click="handleDownload('merged')" :loading="downloading.merged">
                  <el-icon><Download /></el-icon> {{ $t('visualization.downloadCard.downloadBtn') }}
                </el-button>
              </div>
              
              <div class="download-item" v-if="downloadFiles.prodigal">
                <div class="download-info">
                  <el-icon class="file-icon" style="color: #E6A23C;"><FolderOpened /></el-icon>
                  <div class="file-details">
                    <span class="file-name">{{ $t('visualization.downloadCard.prodigalFileName') }}</span>
                    <span class="file-desc">{{ $i18n.locale === 'en' ? $t('visualization.downloadCard.prodigalFileDescEn') : (downloadFiles.prodigal.name + ' ' + $t('visualization.downloadCard.prodigalFileDescSuffix')) }}</span>
                    <span class="file-size">{{ downloadFiles.prodigal.sizeFormatted }}</span>
                  </div>
                </div>
                <el-button type="warning" size="small" @click="handleDownload('prodigal')" :loading="downloading.prodigal">
                  <el-icon><Download /></el-icon> {{ $t('visualization.downloadCard.downloadBtn') }}
                </el-button>
              </div>
            </template>
            
            <div class="download-item download-all">
              <div class="download-info">
                <el-icon class="file-icon" style="color: #409EFF;"><Files /></el-icon>
                <div class="file-details">
                  <span class="file-name">{{ $t('visualization.downloadCard.allFileName') }}</span>
                  <span class="file-desc">{{ $t('visualization.downloadCard.allFileDesc') }}</span>
                </div>
              </div>
              <el-button type="primary" @click="handleDownload('all')" :loading="downloading.all">
                <el-icon><Download /></el-icon> {{ $t('visualization.downloadCard.downloadAllBtn') }}
              </el-button>
            </div>
          </div>
        </div>
      </el-card>
    </template>
    
    <!-- BLAST 结果抽屉 -->
    <BlastDrawer
      v-model="blastDrawerVisible"
      :task-id="taskId"
      :sequence-id="currentBlastSequenceId"
      :arg-class="currentBlastArgClass"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Download, Document, Search, FolderOpened, Files, InfoFilled } from '@element-plus/icons-vue'
import { getGenomeVisualization, getVisualizationResults, getClassSummary } from '@/api/visualization'
import { getDownloadableFiles, downloadFile } from '@/api/download'
import { useUserStore } from '@/stores/user'
import * as echarts from 'echarts'
import BlastDrawer from '@/components/BlastDrawer.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { t, locale } = useI18n()

// 状态管理
const taskId = ref(null)
const activeTab = ref('detail')
const loading = ref(false)
const argData = ref(null)
/** 种类图数据（DB 模式下由接口返回） */
const classSummaryFromApi = ref(null)

// 下载相关状态
const downloadLoading = ref(false)
const downloadFiles = ref({})
const isMagTask = ref(false)
const downloading = reactive({
  arg: false,
  merged: false,
  prodigal: false,
  all: false
})

// BLAST 相关状态
const blastDrawerVisible = ref(false)
const currentBlastSequenceId = ref('')
const currentBlastArgClass = ref('')

// 图表 ref
const pieChartRef = ref(null)
const barChartRef = ref(null)
let pieChartInstance = null
let barChartInstance = null

// 分页和筛选状态
const pagination = reactive({
  currentPage: 1,
  pageSize: 100
})
const searchKeyword = ref('')
const filterArgType = ref('all')

// 是否为 DB 分页模式（后端已落库，只返回当前页）
const isDbMode = computed(() => argData.value?.pagination != null)

// 计算属性
const argResults = computed(() => argData.value?.argResults || [])
const genomeInfo = computed(() => argData.value?.genomeInfo || {})
const argPositiveCount = computed(() => {
  const n = genomeInfo.value.argCount
  if (n !== undefined && n !== null) return n
  return argResults.value.filter(r => r.isArg).length
})
const argNegativeCount = computed(() => {
  const n = genomeInfo.value.nonArgCount
  if (n !== undefined && n !== null) return n
  return argResults.value.filter(r => !r.isArg).length
})
const totalCount = computed(() => {
  const n = genomeInfo.value.totalCount
  if (n !== undefined && n !== null) return n
  return argResults.value.length
})

// 筛选后的结果（仅文件模式时前端筛选；DB 模式由后端筛选）
const filteredResults = computed(() => {
  if (isDbMode.value) return argResults.value
  let results = argResults.value
  if (filterArgType.value === 'arg') results = results.filter(r => r.isArg)
  else if (filterArgType.value === 'non-arg') results = results.filter(r => !r.isArg)
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.trim().toLowerCase()
    results = results.filter(r =>
      r.id?.toLowerCase().includes(keyword) ||
      (r.argClass && r.argClass.toLowerCase().includes(keyword))
    )
  }
  return results
})

// 表格总数：DB 模式用接口返回的 pagination.total，否则用筛选后长度
const tableTotal = computed(() => {
  if (isDbMode.value && argData.value?.pagination)
    return argData.value.pagination.total ?? 0
  return filteredResults.value.length
})

// 当前页的数据：DB 模式即当前页结果，文件模式为前端分页
const paginatedResults = computed(() => {
  if (isDbMode.value) return argResults.value
  const start = (pagination.currentPage - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return filteredResults.value.slice(start, end)
})

// 种类图数据：DB 模式用接口 classSummaryFromApi，否则从当前结果计算
const argClassStats = computed(() => {
  if (isDbMode.value && classSummaryFromApi.value && classSummaryFromApi.value.length > 0)
    return classSummaryFromApi.value.map(({ name, value }) => ({ name, value }))
  const stats = {}
  argResults.value.forEach(r => {
    if (r.isArg && r.argClass) stats[r.argClass] = (stats[r.argClass] || 0) + 1
  })
  return Object.entries(stats)
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value)
})

// 初始化
onMounted(async () => {
  taskId.value = route.query.taskId ? parseInt(route.query.taskId) : null
  
  if (taskId.value) {
    await loadData()
  }
  
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
})

// 组件销毁时清理
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  pieChartInstance?.dispose()
  barChartInstance?.dispose()
})

// 监听标签页切换
watch(activeTab, async (newTab) => {
  if (newTab === 'charts') {
    if (isDbMode.value && taskId.value) {
      try {
        const res = await getClassSummary(taskId.value)
        const data = res.data || res
        classSummaryFromApi.value = data.classSummary || []
      } catch (e) {
        console.error('getClassSummary failed', e)
        classSummaryFromApi.value = []
      }
    } else {
      classSummaryFromApi.value = null
    }
    await nextTick()
    initCharts()
  }
})

// 监听语言切换，重新渲染图表文案
watch(locale, () => {
  if (activeTab.value === 'charts') {
    nextTick(() => initCharts())
  }
})

// 处理窗口大小变化
function handleResize() {
  pieChartInstance?.resize()
  barChartInstance?.resize()
}

// 处理标签页点击
function handleTabClick() {
  // 标签页切换由 watch 处理
}

// 处理搜索
function handleSearch() {
  pagination.currentPage = 1
  if (isDbMode.value) fetchResultsPage()
}

// 处理筛选
function handleFilter() {
  pagination.currentPage = 1
  if (isDbMode.value) fetchResultsPage()
}

// 处理 BLAST 比对
function handleBlast(row) {
  currentBlastSequenceId.value = row.id
  currentBlastArgClass.value = row.argClass || ''
  blastDrawerVisible.value = true
}

// 格式化概率（与预测概率一致，保留两位小数）
function formatProb(prob) {
  const num = Number(prob)
  if (Number.isNaN(num)) return '-'
  return (num * 100).toFixed(2)
}

// 获取进度条颜色（根据概率值）
function getProgressColor(percentage) {
  if (percentage >= 70) return '#67C23A'  // 绿色 - 高置信度
  if (percentage >= 50) return '#409EFF'  // 蓝色 - 中置信度
  if (percentage >= 30) return '#E6A23C'  // 橙色 - 低置信度
  return '#909399'  // 灰色 - 很低置信度
}

// 拉取分页结果（DB 模式）
async function fetchResultsPage() {
  if (!taskId.value || !isDbMode.value) return
  loading.value = true
  try {
    const isArg = filterArgType.value === 'arg' ? true : filterArgType.value === 'non-arg' ? false : undefined
    const keyword = searchKeyword.value?.trim() || undefined
    const res = await getVisualizationResults(taskId.value, {
      page: pagination.currentPage,
      pageSize: pagination.pageSize,
      isArg,
      keyword
    })
    const data = res.data || res
    if (argData.value) {
      argData.value.argResults = data.argResults || []
      argData.value.pagination = data.pagination || {}
    }
  } catch (e) {
    console.error('fetchResultsPage failed', e)
    ElMessage.error(t('visualization.messages.loadFailed') + ': ' + (e.response?.data?.message || e.message))
  } finally {
    loading.value = false
  }
}

// 处理页码变化
function handlePageChange(page) {
  pagination.currentPage = page
  if (isDbMode.value) fetchResultsPage()
}

// 处理每页数量变化
function handlePageSizeChange(size) {
  pagination.pageSize = size
  pagination.currentPage = 1
  if (isDbMode.value) fetchResultsPage()
}

// 加载数据
async function loadData() {
  loading.value = true
  classSummaryFromApi.value = null
  try {
    const response = await getGenomeVisualization(taskId.value)
    argData.value = response.data
    if (argData.value?.pagination) {
      pagination.currentPage = argData.value.pagination.page ?? 1
      pagination.pageSize = argData.value.pagination.pageSize ?? 100
    }
    await loadDownloadableFiles()
    ElMessage.success(t('visualization.messages.loadSuccess'))
  } catch (error) {
    console.error('Failed to load data:', error)
    ElMessage.error(t('visualization.messages.loadFailed') + ': ' + (error.response?.data?.message || error.message))
  } finally {
    loading.value = false
  }
}

// 加载可下载文件列表
async function loadDownloadableFiles() {
  downloadLoading.value = true
  try {
    const response = await getDownloadableFiles(taskId.value)
    // response 是 { code: 0, data: { taskId, isMagTask, files } }
    const data = response.data || response
    isMagTask.value = data.isMagTask || false
    
    // 整理文件信息
    const files = data.files || []
    downloadFiles.value = {}
    files.forEach(file => {
      if (file.key === 'arg_predictions') {
        downloadFiles.value.arg = file
      } else if (file.key === 'merged_faa') {
        downloadFiles.value.merged = file
      } else if (file.key === 'prodigal_results') {
        downloadFiles.value.prodigal = file
      }
    })
  } catch (error) {
    console.error('Failed to load downloadable files:', error)
  } finally {
    downloadLoading.value = false
  }
}

// 处理下载
async function handleDownload(type) {
  downloading[type] = true
  try {
    const token = userStore.token ? `Bearer ${userStore.token}` : ''
    await downloadFile(type, taskId.value, token)
    ElMessage.success(t('visualization.messages.downloadSuccess'))
  } catch (error) {
    console.error('Download failed:', error)
    ElMessage.error(t('visualization.messages.downloadFailed') + ': ' + error.message)
  } finally {
    downloading[type] = false
  }
}

// 下载 ARG 预测结果（DB 模式走后端文件下载，否则用当前数据生成 TSV）
async function downloadArgResults() {
  try {
    if (totalCount.value === 0) {
      ElMessage.warning(t('visualization.detail.noResults'))
      return
    }
    if (isDbMode.value) {
      downloading.arg = true
      const token = userStore.token ? `Bearer ${userStore.token}` : ''
      await downloadFile('arg', taskId.value, token)
      ElMessage.success(t('visualization.messages.downloadSuccess'))
      return
    }
    ElMessage.info(t('visualization.messages.preparingDownload'))
    let tsvContent = 'id\tis_arg\tpred_prob\targ_class\tclass_prob\tprob\n'
    argResults.value.forEach(result => {
      tsvContent += `${result.id || ''}\t${result.isArg ? 'True' : 'False'}\t${result.predProb ?? ''}\t${result.argClass || ''}\t${result.classProb ?? ''}\t${result.prob ?? ''}\n`
    })
    const blob = new Blob([tsvContent], { type: 'text/tab-separated-values;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `task_${taskId.value}_arg_predictions.tsv`
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success(t('visualization.messages.downloadSuccess'))
  } catch (error) {
    console.error('download error:', error)
    ElMessage.error(t('visualization.messages.downloadFailed') + ': ' + error.message)
  } finally {
    downloading.arg = false
  }
}

// 获取 ARG 行的类名（用于设置背景色）
function getArgRowClassName({ row }) {
  return row.isArg ? 'arg-row-positive' : 'arg-row-negative'
}

// 初始化所有图表
function initCharts() {
  initPieChart()
  initBarChart()
}

// 初始化饼图 - ARG 与非 ARG 数量分布
function initPieChart() {
  if (!pieChartRef.value) return
  
  if (!pieChartInstance) {
    pieChartInstance = echarts.init(pieChartRef.value)
  }
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: 'rgba(0, 136, 204, 0.5)',
      borderWidth: 2,
      textStyle: {
        color: '#2c3e50'
      },
      formatter: (params) => {
        const unit = t('visualization.chartPie.labelUnit')
        return `<strong style="color: #0088cc;">${params.name}</strong><br/>${t('visualization.chartPie.tooltipCount')}: ${params.value}${unit ? ' ' + unit : ''}<br/>${t('visualization.chartPie.tooltipPercent')}: ${params.percent}%`
      }
    },
    legend: {
      orient: 'horizontal',
      bottom: 20,
      textStyle: {
        color: '#2c3e50',
        fontSize: 14,
        fontWeight: 500
      },
      itemWidth: 20,
      itemHeight: 14
    },
    series: [
      {
        name: t('visualization.chartPie.seriesName'),
        type: 'pie',
        radius: ['35%', '65%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 3,
          shadowBlur: 15,
          shadowColor: 'rgba(0, 0, 0, 0.15)'
        },
        label: {
          show: true,
          position: 'outside',
          formatter: (params) => {
            const unit = t('visualization.chartPie.labelUnit')
            return params.name + '\n' + params.value + (unit ? ' ' + unit : '') + ' (' + params.percent + '%)'
          },
          fontSize: 13,
          fontWeight: 600,
          color: '#2c3e50',
          lineHeight: 20
        },
        labelLine: {
          show: true,
          length: 20,
          length2: 30,
          lineStyle: {
            color: 'rgba(0, 136, 204, 0.5)',
            width: 2
          }
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          },
          itemStyle: {
            shadowBlur: 25,
            shadowColor: 'rgba(0, 136, 204, 0.4)'
          }
        },
        data: [
          { 
            value: argPositiveCount.value, 
            name: t('visualization.chartPie.argName'),
            itemStyle: { 
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#67C23A' },
                { offset: 1, color: '#95d475' }
              ])
            }
          },
          { 
            value: argNegativeCount.value, 
            name: t('visualization.chartPie.nonArgName'),
            itemStyle: { 
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#909399' },
                { offset: 1, color: '#C0C4CC' }
              ])
            }
          }
        ]
      }
    ]
  }
  
  pieChartInstance.setOption(option, true)
}

// 初始化柱状图 - 各 ARG 类别分布
function initBarChart() {
  if (!barChartRef.value) return
  
  if (!barChartInstance) {
    barChartInstance = echarts.init(barChartRef.value)
  }
  
  const stats = argClassStats.value
  
  if (stats.length === 0) {
    barChartInstance.clear()
    return
  }
  
  // 生成渐变色
  const colors = [
    ['#409EFF', '#79bbff'],
    ['#67C23A', '#95d475'],
    ['#E6A23C', '#eebe77'],
    ['#F56C6C', '#fab6b6'],
    ['#9C27B0', '#ce93d8'],
    ['#00BCD4', '#4dd0e1'],
    ['#FF5722', '#ff8a65'],
    ['#795548', '#a1887f'],
    ['#607D8B', '#90a4ae'],
    ['#3F51B5', '#7986cb']
  ]
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: 'rgba(0, 136, 204, 0.5)',
      borderWidth: 2,
      textStyle: {
        color: '#2c3e50'
      },
      axisPointer: {
        type: 'shadow',
        shadowStyle: {
          color: 'rgba(0, 136, 204, 0.1)'
        }
      },
      formatter: (params) => {
        const data = params[0]
        const unit = t('visualization.chartPie.labelUnit')
        return `<strong style="color: #0088cc;">${data.name}</strong><br/>${t('visualization.chartBar.tooltipCount')}: ${data.value}${unit ? ' ' + unit : ''}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: stats.length > 5 ? '25%' : '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: stats.map(s => s.name),
      axisLabel: {
        color: '#2c3e50',
        fontSize: 12,
        fontWeight: 500,
        rotate: stats.length > 5 ? 45 : 0,
        interval: 0
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(0, 136, 204, 0.3)'
        }
      },
      axisTick: {
        alignWithLabel: true,
        lineStyle: {
          color: 'rgba(0, 136, 204, 0.3)'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: t('visualization.chartBar.yAxisName'),
      nameTextStyle: {
        color: '#0088cc',
        fontSize: 13,
        fontWeight: 600
      },
      axisLabel: {
        color: '#606266',
        fontSize: 12
      },
      axisLine: {
        show: true,
        lineStyle: {
          color: 'rgba(0, 136, 204, 0.3)'
        }
      },
      splitLine: {
        lineStyle: {
          type: 'dashed',
          color: 'rgba(0, 136, 204, 0.15)'
        }
      }
    },
    series: [
      {
        name: t('visualization.chartBar.seriesName'),
        type: 'bar',
        barWidth: stats.length > 8 ? '50%' : '40%',
        data: stats.map((s, index) => ({
          value: s.value,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: colors[index % colors.length][0] },
              { offset: 1, color: colors[index % colors.length][1] }
            ]),
            borderRadius: [6, 6, 0, 0],
            shadowBlur: 8,
            shadowColor: 'rgba(0, 0, 0, 0.15)',
            shadowOffsetY: 4
          }
        })),
        label: {
          show: true,
          position: 'top',
          color: '#0088cc',
          fontSize: 12,
          fontWeight: 600
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 15,
            shadowColor: 'rgba(0, 136, 204, 0.4)'
          }
        }
      }
    ],
    dataZoom: stats.length > 10 ? [{
      type: 'slider',
      xAxisIndex: 0,
      start: 0,
      end: Math.min(100, (10 / stats.length) * 100),
      height: 20,
      bottom: 5
    }] : []
  }
  
  barChartInstance.setOption(option, true)
}

// 下载图表图片
function downloadChartImages() {
  try {
    ElMessage.info(t('visualization.messages.chartsGenerating'))
    
    let downloadCount = 0
    
    // 下载饼图
    if (pieChartInstance) {
      const pieUrl = pieChartInstance.getDataURL({
        type: 'png',
        pixelRatio: 2,
        backgroundColor: '#ffffff'
      })
      const a1 = document.createElement('a')
      a1.href = pieUrl
      a1.download = `task_${taskId.value}_arg_distribution_pie.png`
      a1.click()
      downloadCount++
    }
    
    // 下载柱状图
    if (barChartInstance && argClassStats.value.length > 0) {
      setTimeout(() => {
        const barUrl = barChartInstance.getDataURL({
          type: 'png',
          pixelRatio: 2,
          backgroundColor: '#ffffff'
        })
        const a2 = document.createElement('a')
        a2.href = barUrl
        a2.download = `task_${taskId.value}_arg_class_distribution_bar.png`
        a2.click()
        
        ElMessage.success(t('visualization.messages.chartsDownloadSuccess'))
      }, 500)
    } else {
      if (downloadCount > 0) {
        ElMessage.success(t('visualization.messages.pieDownloadSuccess'))
      } else {
        ElMessage.warning(t('visualization.messages.noChartsToDownload'))
      }
    }
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error(t('visualization.messages.downloadFailed') + ': ' + error.message)
  }
}
</script>

<style scoped>
.visualization-container {
  padding: 20px;
}

/* 亮色主题 - 与科技青色主题搭配 */
:deep(.el-card) {
  background: linear-gradient(135deg, #ffffff 0%, #f8feff 100%);
  border: 2px solid rgba(0, 180, 255, 0.2);
  box-shadow: 0 4px 20px rgba(0, 180, 255, 0.1);
  color: #2c3e50;
}

:deep(.el-card__header) {
  background: linear-gradient(135deg, rgba(0, 180, 255, 0.08) 0%, rgba(0, 200, 255, 0.12) 100%);
  border-bottom: 2px solid rgba(0, 180, 255, 0.25);
  color: #0088cc;
  font-weight: 600;
}

:deep(.el-descriptions__label) {
  color: #0088cc;
  background: rgba(0, 180, 255, 0.08);
  font-weight: 600;
}

:deep(.el-descriptions__content) {
  color: #2c3e50;
  background: #ffffff;
}

:deep(.el-table) {
  background: #ffffff;
  color: #2c3e50;
}

:deep(.el-table th.el-table__cell) {
  background: linear-gradient(135deg, rgba(0, 180, 255, 0.12) 0%, rgba(0, 200, 255, 0.18) 100%);
  color: #0088cc;
  border-bottom: 2px solid rgba(0, 180, 255, 0.3);
  font-weight: 600;
}

:deep(.el-table tr) {
  background: #ffffff;
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(0, 180, 255, 0.1);
  color: #2c3e50;
}

:deep(.el-tabs__item) {
  color: #606266;
  font-weight: 500;
}

:deep(.el-tabs__item.is-active) {
  color: #0088cc;
  font-weight: 600;
}

:deep(.el-tabs__active-bar) {
  background-color: #0088cc;
}

h3, h4, h5 {
  color: #0088cc;
  text-shadow: 0 2px 4px rgba(0, 136, 204, 0.15);
  font-weight: 600;
}

.info-header {
  margin-bottom: 20px;
}

.tabs-card {
  margin-top: 20px;
}

/* 下载卡片样式 */
.download-card {
  margin-top: 20px;
}

.download-card .card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #0088cc;
}

.download-card .card-header .el-icon {
  font-size: 18px;
}

.download-content {
  padding: 10px 0;
}

.download-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.download-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: rgba(0, 136, 204, 0.03);
  border: 1px solid rgba(0, 136, 204, 0.15);
  border-radius: 8px;
  transition: all 0.3s ease;
}

.download-item:hover {
  background: rgba(0, 136, 204, 0.08);
  border-color: rgba(0, 136, 204, 0.25);
}

.download-item.download-all {
  background: rgba(64, 158, 255, 0.05);
  border-color: rgba(64, 158, 255, 0.2);
}

.download-item.download-all:hover {
  background: rgba(64, 158, 255, 0.1);
  border-color: rgba(64, 158, 255, 0.3);
}

.download-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.download-info .file-icon {
  font-size: 32px;
  color: #0088cc;
}

.file-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-details .file-name {
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.file-details .file-desc {
  color: #606266;
  font-size: 12px;
}

.file-details .file-size {
  color: #909399;
  font-size: 11px;
}

.detail-content {
  padding: 20px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.summary-desc {
  color: #606266;
  font-size: 14px;
  margin: 8px 0 16px;
}

h3 {
  margin: 0 0 16px;
  font-size: 18px;
  color: #303133;
}

/* 图例样式 */
.legend-container {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
  color: #2c3e50;
}

.legend-item {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #2c3e50;
  font-weight: 500;
}

.legend-box {
  display: inline-block;
  width: 20px;
  height: 14px;
  margin-right: 6px;
  border: 1px solid #dcdfe6;
  border-radius: 2px;
}

/* ARG 结果表格颜色 */
.arg-positive-box {
  background-color: #d4edda;  /* 绿色 - 是抗性基因 */
}

.arg-negative-box {
  background-color: #E4E7ED;  /* 灰色 - 不是抗性基因 */
}

/* ARG 表格行颜色 */
:deep(.arg-row-positive > td.el-table__cell) {
  background-color: #d4edda !important;  /* 绿色 - 是抗性基因 */
}

:deep(.arg-row-negative > td.el-table__cell) {
  background-color: #E4E7ED !important;  /* 灰色 - 不是抗性基因 */
}

/* BLAST 按钮样式 */
.blast-btn {
  background: linear-gradient(135deg, #0088cc 0%, #00b4ff 100%);
  border: none;
  font-weight: 500;
  padding: 4px 10px;
  transition: all 0.3s ease;
}

.blast-btn:hover {
  background: linear-gradient(135deg, #0077b3 0%, #0099dd 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 136, 204, 0.3);
}

.blast-btn:active {
  transform: translateY(0);
}

/* ARG 分类标签样式 */
.arg-class-tag {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s ease;
}

.arg-class-tag:hover {
  background-color: rgba(0, 136, 204, 0.15);
  border-color: #0088cc;
}

.arg-class-tag .info-icon {
  font-size: 12px;
  color: #909399;
  transition: color 0.2s ease;
}

.arg-class-tag:hover .info-icon {
  color: #0088cc;
}

/* Top-5 分类弹窗样式 */
.top-classes-content {
  padding: 4px 0;
}

.top-classes-content .popover-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
  font-size: 14px;
}

.top-classes-content .top-class-item {
  display: flex;
  align-items: center;
  padding: 6px 4px;
  border-radius: 4px;
  transition: background-color 0.2s ease;
}

.top-classes-content .top-class-item:hover {
  background-color: rgba(0, 136, 204, 0.05);
}

.top-classes-content .top-class-first {
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.1) 0%, rgba(103, 194, 58, 0.15) 100%);
  border-left: 3px solid #67C23A;
  margin-left: -4px;
  padding-left: 8px;
}

.top-classes-content .rank {
  width: 24px;
  color: #909399;
  font-weight: 500;
  font-size: 13px;
}

.top-classes-content .class-name {
  flex: 1;
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.top-classes-content .top-class-first .class-name {
  color: #67C23A;
  font-weight: 600;
}

.top-classes-content .prob-value {
  width: 50px;
  text-align: right;
  font-family: 'Monaco', 'Menlo', monospace;
  color: #409EFF;
  font-weight: 500;
  font-size: 12px;
}

.top-classes-content .top-class-first .prob-value {
  color: #67C23A;
}

/* 分类概率文本样式 */
.class-prob-text {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  font-weight: 600;
  color: #409EFF;
}

/* 表格工具栏样式 */
.table-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(0, 180, 255, 0.05) 0%, rgba(0, 200, 255, 0.08) 100%);
  border-radius: 8px;
  border: 1px solid rgba(0, 180, 255, 0.15);
}

.filter-info {
  margin-left: auto;
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

/* 分页样式 */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 180, 255, 0.15);
}

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-hover-color: #0088cc;
}

:deep(.el-pagination .el-pager li) {
  background: rgba(0, 180, 255, 0.05);
  color: #2c3e50;
  border: 1px solid rgba(0, 180, 255, 0.2);
  border-radius: 4px;
  margin: 0 2px;
}

:deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #0088cc 0%, #00b4ff 100%);
  color: #ffffff;
  border-color: #0088cc;
}

:deep(.el-pagination .el-pager li:hover:not(.is-active)) {
  color: #0088cc;
  background: rgba(0, 180, 255, 0.1);
}

/* 结果可视化初始页面的空状态样式：外层卡片和内部都用主题背景 */
.visualization-container .empty-state-card {
  background: var(--theme-gradient-bg);
  border-color: var(--theme-border-2);
  box-shadow: 0 4px 20px var(--theme-shadow);
}
.visualization-container .empty-state-card :deep(.el-card__body) {
  background: var(--theme-gradient-bg);
  padding: 40px;
}
.visualization-container :deep(.el-empty) {
  background: var(--theme-gradient-bg);
}

.visualization-container :deep(.empty-state-primary-btn) {
  background: var(--theme-btn-primary) !important;
  border-color: var(--theme-btn-primary) !important;
  color: var(--theme-btn-primary-text) !important;
}
.visualization-container :deep(.empty-state-primary-btn:hover) {
  background: var(--theme-btn-primary-hover) !important;
  border-color: var(--theme-btn-primary-hover) !important;
  color: var(--theme-btn-primary-text) !important;
}

.visualization-container :deep(.el-empty__image svg) {
  fill: rgba(0, 180, 255, 0.4) !important;
}

.visualization-container :deep(.el-empty__image) {
  filter: none;
  opacity: 1;
}

.visualization-container :deep(.el-empty__description) {
  color: var(--theme-accent);
}

.visualization-container :deep(.el-empty__image path) {
  fill: rgba(var(--theme-border-rgb), 0.4) !important;
  stroke: var(--theme-border-5) !important;
}

/* 可视化图表标签页样式 */
.charts-content {
  padding: 20px;
}

.charts-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 24px;
}

.charts-header h3 {
  margin: 0;
  font-size: 20px;
  color: #0088cc;
  font-weight: 600;
}

.charts-desc {
  color: #606266;
  font-size: 14px;
  margin: 8px 0 0;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 24px;
}

@media (max-width: 1100px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
}

.chart-container {
  background: linear-gradient(135deg, rgba(0, 180, 255, 0.03) 0%, rgba(0, 200, 255, 0.06) 100%);
  padding: 20px;
  border-radius: 12px;
  border: 2px solid rgba(0, 180, 255, 0.2);
  box-shadow: 0 4px 16px rgba(0, 180, 255, 0.1);
  transition: all 0.3s ease;
}

.chart-container:hover {
  border-color: rgba(0, 180, 255, 0.4);
  box-shadow: 0 6px 24px rgba(0, 180, 255, 0.15);
}

.chart-container h4 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #0088cc;
  font-weight: 600;
}

.chart-container .chart-desc {
  color: #909399;
  font-size: 13px;
  margin: 0 0 16px;
  line-height: 1.5;
}

.chart {
  width: 100%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(248, 254, 255, 0.95) 100%);
  border-radius: 8px;
  border: 1px solid rgba(0, 180, 255, 0.15);
}
</style>

