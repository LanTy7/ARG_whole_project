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
                  @expand-change="handleExpandChange"
                >
                  <!-- 展开列：显示氨基酸序列 -->
                  <el-table-column type="expand" width="50">
                    <template #default="{ row }">
                      <div class="sequence-expand-content" v-loading="sequenceLoading[row.id]">
                        <div class="sequence-header">
                          <div class="sequence-info">
                            <el-icon><Document /></el-icon>
                            <span class="sequence-id">{{ row.id }}</span>
                            <el-tag type="info" size="small" v-if="sequenceData[row.id]">
                              {{ sequenceData[row.id].length }} aa
                            </el-tag>
                          </div>
                          <el-button 
                            type="primary" 
                            size="small" 
                            :icon="DocumentCopy"
                            @click="copySequence(row.id)"
                            v-if="sequenceData[row.id]"
                          >
                            {{ $t('visualization.copySequence') || '复制序列' }}
                          </el-button>
                        </div>
                        <div class="sequence-body" v-if="sequenceData[row.id]">
                          <pre class="sequence-text">{{ formatSequence(sequenceData[row.id]) }}</pre>
                        </div>
                        <el-empty v-else-if="!sequenceLoading[row.id]" :description="$t('visualization.noSequenceData') || '暂无序列数据'" />
                      </div>
                    </template>
                  </el-table-column>
                  
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
                        type="primary"
                        size="small"
                        :icon="Search"
                        @click="handleBlast(row)"
                        class="blast-btn"
                      >
                        BLAST
                      </el-button>
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
              
              <div v-else>
                <!-- 统计概览卡片 -->
                <div class="stats-overview">
                  <div class="stat-card-teal">
                    <div class="stat-icon-bg">
                      <el-icon><Document /></el-icon>
                    </div>
                    <div class="stat-info">
                      <div class="stat-value-teal">{{ totalCount }}</div>
                      <div class="stat-label-teal">{{ $t('visualization.statsOverview.totalSequences') }}</div>
                    </div>
                  </div>
                  <div class="stat-card-teal arg">
                    <div class="stat-icon-bg arg">
                      <el-icon><CircleCheck /></el-icon>
                    </div>
                    <div class="stat-info">
                      <div class="stat-value-teal arg">{{ argPositiveCount }}</div>
                      <div class="stat-label-teal">{{ $t('visualization.statsOverview.argSequences') }}</div>
                    </div>
                  </div>
                  <div class="stat-card-teal non-arg">
                    <div class="stat-icon-bg non-arg">
                      <el-icon><CircleClose /></el-icon>
                    </div>
                    <div class="stat-info">
                      <div class="stat-value-teal non-arg">{{ argNegativeCount }}</div>
                      <div class="stat-label-teal">{{ $t('visualization.statsOverview.nonArgSequences') }}</div>
                    </div>
                  </div>
                  <div class="stat-card-teal ratio">
                    <div class="stat-icon-bg ratio">
                      <el-icon><DataAnalysis /></el-icon>
                    </div>
                    <div class="stat-info">
                      <div class="stat-value-teal ratio">{{ ((argPositiveCount / totalCount) * 100).toFixed(1) }}%</div>
                      <div class="stat-label-teal">{{ $t('visualization.statsOverview.argRatio') }}</div>
                    </div>
                  </div>
                </div>
                
                <div class="charts-grid">
                  <div class="chart-container enhanced">
                    <div class="chart-header">
                      <h4>{{ $t('visualization.chartsPage.pieTitle') }}</h4>
                      <p class="chart-desc">{{ $t('visualization.chartsPage.pieDesc') }}</p>
                    </div>
                    <div ref="pieChartRef" class="chart" style="height: 400px;"></div>
                  </div>
                  
                  <div class="chart-container enhanced">
                    <div class="chart-header">
                      <h4>{{ $t('visualization.chartsPage.barTitle') }}</h4>
                      <p class="chart-desc">{{ $t('visualization.chartsPage.barDesc') }}</p>
                    </div>
                    <div ref="barChartRef" class="chart" style="height: 400px;"></div>
                    <el-empty v-if="argClassStats.length === 0 && argPositiveCount > 0" :description="$t('visualization.chartsPage.noCategory')" />
                  </div>
                </div>
                
                <!-- ARG 关系网络图 -->
                <div class="chart-container enhanced network-chart-container">
                  <div class="chart-header">
                    <h4>{{ $t('visualization.networkChart.title') || 'ARG 关系网络图' }}</h4>
                    <p class="chart-desc">
                      {{ $t('visualization.networkChart.desc') || '展示 ARG 类别与代表性序列的关系网络' }}
                      (共 {{ allArgSequences ? allArgSequences.length : argPositiveCount }} 个序列
                      <el-tag v-if="(allArgSequences ? allArgSequences.length : argPositiveCount) > 200" 
                              type="warning" size="small" style="margin-left: 8px;">
                        仅显示概率最高的代表性序列
                      </el-tag>
                      )
                    </p>
                    <!-- 操作说明 -->
                    <div class="network-controls-hint">
                      <el-alert type="info" :closable="false" class="network-hint-alert">
                        <template #title>
                          <div class="hint-content">
                            <span class="hint-item">
                              <el-icon><ZoomIn /></el-icon>
                              <span>{{ $t('visualization.networkChart.hintZoom') || '滚轮/双指捏合：缩放' }}</span>
                            </span>
                            <span class="hint-item">
                              <el-icon><Rank /></el-icon>
                              <span>{{ $t('visualization.networkChart.hintPan') || '拖拽/单指滑动：平移' }}</span>
                            </span>
                            <span class="hint-item">
                              <el-icon><Mouse /></el-icon>
                              <span>{{ $t('visualization.networkChart.hintClick') || '点击节点：查看详情' }}</span>
                            </span>
                          </div>
                        </template>
                      </el-alert>
                      <!-- 缩放控制按钮 -->
                      <div class="zoom-controls">
                        <el-button-group>
                          <el-button size="small" @click="zoomNetwork(1.2)">
                            <el-icon><ZoomIn /></el-icon>
                          </el-button>
                          <el-button size="small" @click="resetNetworkZoom">
                            <el-icon><Refresh /></el-icon>
                            {{ $t('visualization.networkChart.reset') || '重置' }}
                          </el-button>
                          <el-button size="small" @click="zoomNetwork(0.8)">
                            <el-icon><ZoomOut /></el-icon>
                          </el-button>
                        </el-button-group>
                      </div>
                    </div>
                  </div>
                  <div v-if="argPositiveCount === 0" class="network-empty">
                    <el-empty :description="$t('visualization.networkChart.noData') || '暂无抗性基因数据'" />
                  </div>
                  <div v-else-if="networkChartLoading" class="network-empty" v-loading="networkChartLoading">
                    <el-empty :description="$t('visualization.networkChart.loading') || '正在加载网络图数据...'" />
                  </div>
                  <div v-else ref="networkChartRef" class="chart network-chart" style="height: 1000px;"></div>
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
import { Download, Document, Search, FolderOpened, Files, InfoFilled, DocumentCopy, CircleCheck, CircleClose, DataAnalysis, ZoomIn, ZoomOut, Rank, Mouse, Refresh } from '@element-plus/icons-vue'
import { getGenomeVisualization, getVisualizationResults, getClassSummary, getAllArgSequences } from '@/api/visualization'
import { getSequence } from '@/api/blast'
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
/** 所有 ARG 序列（用于网络图展示，DB 模式下会单独获取全部数据） */
const allArgSequences = ref(null)
/** 网络图数据加载状态 */
const networkChartLoading = ref(false)

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

// 序列查看相关状态
const sequenceData = ref({})
const sequenceLoading = ref({})

// 图表 ref
const pieChartRef = ref(null)
const barChartRef = ref(null)
const networkChartRef = ref(null)
let pieChartInstance = null
let barChartInstance = null
let networkChartInstance = null

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
  networkChartInstance?.dispose()
})

// 监听标签页切换
watch(activeTab, async (newTab) => {
  if (newTab === 'charts') {
    // 重置网络图数据
    allArgSequences.value = null
    
    if (isDbMode.value && taskId.value) {
      // DB 模式下需要单独获取类别统计和所有 ARG 序列（用于网络图）
      networkChartLoading.value = true
      try {
        const [classRes, argRes] = await Promise.all([
          getClassSummary(taskId.value),
          getAllArgSequences(taskId.value)
        ])
        classSummaryFromApi.value = classRes.data?.classSummary || []
        allArgSequences.value = argRes.data?.argSequences || []
      } catch (e) {
        console.error('Failed to load chart data:', e)
        classSummaryFromApi.value = []
        allArgSequences.value = []
      } finally {
        networkChartLoading.value = false
      }
    } else {
      // 文件模式下使用已有数据
      classSummaryFromApi.value = null
      allArgSequences.value = null
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
  networkChartInstance?.resize()
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

// 处理表格行展开/收起
async function handleExpandChange(row, expandedRows) {
  const rowId = row.id
  // 如果展开且还没有加载过序列数据
  if (expandedRows.includes(row) && !sequenceData.value[rowId]) {
    sequenceLoading.value[rowId] = true
    try {
      const res = await getSequence(taskId.value, rowId)
      sequenceData.value[rowId] = res.data?.sequence || res.data
    } catch (error) {
      console.error('获取序列失败:', error)
      ElMessage.error(t('visualization.sequenceLoadFailed') || '获取序列失败')
      sequenceData.value[rowId] = null
    } finally {
      sequenceLoading.value[rowId] = false
    }
  }
}

// 格式化序列显示（每60个字符一行）
function formatSequence(sequence) {
  if (!sequence) return ''
  // 移除所有空白字符
  const cleanSeq = sequence.replace(/\s/g, '')
  // 每60个字符一行
  const lines = []
  for (let i = 0; i < cleanSeq.length; i += 60) {
    lines.push(cleanSeq.substring(i, i + 60))
  }
  return lines.join('\n')
}

// 复制序列到剪贴板
async function copySequence(sequenceId) {
  const sequence = sequenceData.value[sequenceId]
  if (!sequence) return
  
  try {
    await navigator.clipboard.writeText(sequence)
    ElMessage.success(t('visualization.sequenceCopied') || '序列已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    // 降级方案
    const textarea = document.createElement('textarea')
    textarea.value = sequence
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success(t('visualization.sequenceCopied') || '序列已复制到剪贴板')
  }
}

// 格式化概率（与预测概率一致，保留两位小数）
function formatProb(prob) {
  const num = Number(prob)
  if (Number.isNaN(num)) return '-'
  return (num * 100).toFixed(2)
}

// 获取进度条颜色（根据概率值）
function getProgressColor(percentage) {
  if (percentage >= 70) return '#2a9d8f'  // 青绿色 - 高置信度
  if (percentage >= 50) return '#3dccc7'  // 浅青绿 - 中置信度
  if (percentage >= 30) return '#e9c46a'  // 橙黄色 - 低置信度
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
  initNetworkChart()
}

// 初始化饼图 - ARG 与非 ARG 数量分布（科技感环形图）
function initPieChart() {
  if (!pieChartRef.value) return
  
  if (!pieChartInstance) {
    pieChartInstance = echarts.init(pieChartRef.value)
  }
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#2a9d8f',
      borderWidth: 1,
      textStyle: {
        color: '#1a3a36'
      },
      formatter: (params) => {
        const unit = t('visualization.chartPie.labelUnit') || ''
        const countLabel = t('visualization.chartPie.tooltipCount') || 'Count'
        const percentLabel = t('visualization.chartPie.tooltipPercent') || 'Percent'
        return `<div style="font-weight:600;color:#2a9d8f;margin-bottom:4px;">${params.name}</div>
                <div>${countLabel}: <strong>${params.value}</strong> ${unit}</div>
                <div>${percentLabel}: <strong>${params.percent}%</strong></div>`
      }
    },
    legend: {
      orient: 'horizontal',
      bottom: 15,
      itemGap: 20,
      textStyle: {
        color: '#1a3a36',
        fontSize: 13,
        fontWeight: 500
      },
      itemWidth: 14,
      itemHeight: 14,
      icon: 'circle'
    },
    series: [
      // 外圈装饰
      {
        type: 'pie',
        radius: ['68%', '72%'],
        center: ['50%', '45%'],
        silent: true,
        itemStyle: {
          color: 'rgba(42, 157, 143, 0.1)'
        },
        data: [{ value: 1 }],
        label: { show: false }
      },
      // 主环形图
      {
        name: t('visualization.chartPie.seriesName'),
        type: 'pie',
        radius: ['45%', '65%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 3,
          shadowBlur: 20,
          shadowColor: 'rgba(42, 157, 143, 0.3)'
        },
        label: {
          show: true,
          position: 'outside',
          formatter: (params) => {
            const unit = t('visualization.chartPie.labelUnit') || ''
            return `{name|${params.name}}\n{value|${params.value}${unit}} {percent|${params.percent}%}`
          },
          rich: {
            name: {
              fontSize: 13,
              fontWeight: 600,
              color: '#1a3a36',
              lineHeight: 22
            },
            value: {
              fontSize: 14,
              fontWeight: 700,
              color: '#2a9d8f'
            },
            percent: {
              fontSize: 12,
              color: '#666',
              fontWeight: 500
            }
          }
        },
        labelLine: {
          show: true,
          length: 20,
          length2: 25,
          lineStyle: {
            color: 'rgba(42, 157, 143, 0.4)',
            width: 2
          }
        },
        emphasis: {
          scale: true,
          scaleSize: 10,
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          },
          itemStyle: {
            shadowBlur: 30,
            shadowColor: 'rgba(42, 157, 143, 0.5)'
          }
        },
        data: [
          { 
            value: argPositiveCount.value, 
            name: t('visualization.chartPie.argName'),
            itemStyle: { 
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#2a9d8f' },
                { offset: 0.5, color: '#3dccc7' },
                { offset: 1, color: '#2a9d8f' }
              ]),
              shadowBlur: 20,
              shadowColor: 'rgba(42, 157, 143, 0.4)'
            }
          },
          { 
            value: argNegativeCount.value, 
            name: t('visualization.chartPie.nonArgName'),
            itemStyle: { 
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#a8b5b3' },
                { offset: 0.5, color: '#d1dcd9' },
                { offset: 1, color: '#a8b5b3' }
              ])
            }
          }
        ]
      },
      // 中心文字
      {
        type: 'pie',
        radius: ['0%', '35%'],
        center: ['50%', '45%'],
        silent: true,
        itemStyle: {
          color: 'transparent'
        },
        label: {
          show: true,
          position: 'center',
          formatter: () => {
            return `{total|${totalCount.value}}\n{label|${t('visualization.statsOverview.totalLabel')}}`
          },
          rich: {
            total: {
              fontSize: 36,
              fontWeight: 700,
              color: '#2a9d8f',
              lineHeight: 42
            },
            label: {
              fontSize: 14,
              color: '#666',
              fontWeight: 500
            }
          }
        },
        data: [{ value: 1 }],
        animation: false
      }
    ]
  }
  
  pieChartInstance.setOption(option, true)
}

// 初始化柱状图 - 各 ARG 类别分布（科技感柱状图）
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
  
  // 统一使用青绿色渐变
  const getGradient = (index) => {
    const opacity = 1 - (index * 0.08)
    return new echarts.graphic.LinearGradient(0, 0, 0, 1, [
      { offset: 0, color: `rgba(42, 157, 143, ${Math.max(0.7, opacity)})` },
      { offset: 0.5, color: `rgba(61, 204, 199, ${Math.max(0.5, opacity - 0.2)})` },
      { offset: 1, color: `rgba(42, 157, 143, ${Math.max(0.4, opacity - 0.3)})` }
    ])
  }
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#2a9d8f',
      borderWidth: 1,
      textStyle: {
        color: '#1a3a36'
      },
      axisPointer: {
        type: 'shadow',
        shadowStyle: {
          color: 'rgba(42, 157, 143, 0.15)'
        }
      },
      formatter: (params) => {
        const data = params[0]
        const unit = t('visualization.chartPie.labelUnit') || ''
        const countLabel = t('visualization.chartBar.tooltipCount') || 'Count'
        return `<div style="font-weight:600;color:#2a9d8f;margin-bottom:4px;">${data.name}</div>
                <div>${countLabel}: <strong>${data.value}</strong> ${unit}</div>`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: stats.length > 5 ? '22%' : '12%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: stats.map(s => s.name),
      axisLabel: {
        color: '#1a3a36',
        fontSize: 12,
        fontWeight: 500,
        rotate: stats.length > 5 ? 35 : 0,
        interval: 0
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(42, 157, 143, 0.3)',
          width: 2
        }
      },
      axisTick: {
        alignWithLabel: true,
        lineStyle: {
          color: 'rgba(42, 157, 143, 0.3)'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: t('visualization.chartBar.yAxisName') || '数量',
      nameTextStyle: {
        color: '#2a9d8f',
        fontSize: 13,
        fontWeight: 600,
        padding: [0, 0, 0, -10]
      },
      axisLabel: {
        color: '#666',
        fontSize: 12
      },
      axisLine: {
        show: false
      },
      splitLine: {
        lineStyle: {
          type: 'dashed',
          color: 'rgba(42, 157, 143, 0.1)'
        }
      }
    },
    series: [
      {
        name: t('visualization.chartBar.seriesName'),
        type: 'bar',
        barWidth: stats.length > 8 ? '55%' : '45%',
        data: stats.map((s, index) => ({
          value: s.value,
          itemStyle: {
            color: getGradient(index),
            borderRadius: [8, 8, 0, 0],
            shadowBlur: 10,
            shadowColor: 'rgba(42, 157, 143, 0.3)',
            shadowOffsetY: 5
          }
        })),
        label: {
          show: true,
          position: 'top',
          color: '#2a9d8f',
          fontSize: 13,
          fontWeight: 700,
          formatter: '{c}'
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 20,
            shadowColor: 'rgba(42, 157, 143, 0.5)',
            borderRadius: [10, 10, 0, 0]
          }
        },
        animationDelay: (idx) => idx * 50
      }
    ],
    dataZoom: stats.length > 10 ? [{
      type: 'slider',
      xAxisIndex: 0,
      start: 0,
      end: Math.min(100, (10 / stats.length) * 100),
      height: 20,
      bottom: 5,
      borderColor: 'rgba(42, 157, 143, 0.2)',
      fillerColor: 'rgba(42, 157, 143, 0.15)',
      handleStyle: {
        color: '#2a9d8f'
      }
    }] : []
  }
  
  barChartInstance.setOption(option, true)
}

// 初始化网络图 - ARG 类别与序列关系网络（全新优化版本）
function initNetworkChart() {
  if (!networkChartRef.value) return

  if (!networkChartInstance) {
    networkChartInstance = echarts.init(networkChartRef.value)
  }

  // 使用所有 ARG 序列（如果已加载），否则使用当前页的 ARG 序列
  const sourceData = allArgSequences.value || argResults.value
  const argSequences = sourceData.filter(r => r.isArg)

  if (argSequences.length === 0) {
    networkChartInstance.clear()
    return
  }

  // 按类别分组，并对每个类别的序列按概率排序
  const classGroups = {}
  argSequences.forEach(seq => {
    const className = seq.argClass || 'Unknown'
    if (!classGroups[className]) {
      classGroups[className] = []
    }
    classGroups[className].push(seq)
  })

  // 对每个类别内的序列按预测概率排序（高概率在前）
  Object.keys(classGroups).forEach(className => {
    classGroups[className].sort((a, b) => (b.predProb || 0) - (a.predProb || 0))
  })

  // 类别颜色映射 - 使用更丰富的配色方案
  const classColors = [
    '#2a9d8f', '#e76f51', '#264653', '#e9c46a', '#f4a261',
    '#a8dadc', '#457b9d', '#1d3557', '#e63946', '#a8e6cf',
    '#ffd3b6', '#ff8b94', '#6c5b7b', '#c06c84', '#f67280'
  ]

  // 生成节点和连接
  const nodes = []
  const links = []
  const categories = []

  // 计算布局参数
  const categoryCount = Object.keys(classGroups).length
  const totalArgCount = argSequences.length

  // 采样策略：每个类别显示的序列数
  let maxSeqPerCategory = 50
  if (totalArgCount > 1000) {
    maxSeqPerCategory = 15
  } else if (totalArgCount > 500) {
    maxSeqPerCategory = 25
  } else if (totalArgCount > 200) {
    maxSeqPerCategory = 35
  }

  // ==================== 创建节点 ====================

  // 1. 中心根节点（ARG 总数）- 文字显示在节点内部
  const rootNode = {
    id: 'ARG_ROOT',
    name: `ARG\n${argSequences.length}`,
    symbolSize: 110,
    value: argSequences.length,
    category: 0,
    itemStyle: {
      color: new echarts.graphic.RadialGradient(0.5, 0.5, 0.8, [
        { offset: 0, color: '#3dccc7' },
        { offset: 0.6, color: '#2a9d8f' },
        { offset: 1, color: '#1a7a6f' }
      ]),
      shadowBlur: 50,
      shadowColor: 'rgba(42, 157, 143, 0.6)',
      borderWidth: 3,
      borderColor: '#fff'
    },
    label: {
      show: true,
      position: 'inside',
      distance: 0,
      fontSize: 22,
      fontWeight: 'bold',
      color: '#fff',
      lineHeight: 28,
      formatter: (params) => {
        const count = params.data.value
        return `{title|ARG}\n{count|${count}}`
      },
      rich: {
        title: {
          fontSize: 24,
          fontWeight: 'bold',
          color: '#fff',
          lineHeight: 30,
          align: 'center'
        },
        count: {
          fontSize: 20,
          fontWeight: 'bold',
          color: 'rgba(255,255,255,0.95)',
          lineHeight: 28,
          align: 'center'
        }
      }
    }
  }
  nodes.push(rootNode)

  // 2. 创建类别节点（在根节点周围环形分布）
  let categoryIndex = 1
  const categoryEntries = Object.entries(classGroups)
  const radiusStep = 180 // 类别环半径

  categoryEntries.forEach(([className, sequences], index) => {
    const color = classColors[index % classColors.length]
    const displayCount = Math.min(sequences.length, maxSeqPerCategory)
    const totalCount = sequences.length

    // 类别名称处理
    const displayClassName = className === 'Unknown' || className === 'unknown' ? 'Unknown' : className

    // 类别节点位置（沿圆环分布）
    const angle = (index / categoryCount) * Math.PI * 2 - Math.PI / 2
    const x = Math.cos(angle) * radiusStep
    const y = Math.sin(angle) * radiusStep

    // 类别节点
    const classNodeId = `CLASS_${className}`
    const classNodeSize = Math.min(75, 45 + Math.sqrt(sequences.length) * 4)

    const classNode = {
      id: classNodeId,
      name: displayClassName,
      symbolSize: classNodeSize,
      value: sequences.length,
      category: categoryIndex,
      x: x,
      y: y,
      itemStyle: {
        color: new echarts.graphic.RadialGradient(0.5, 0.5, 0.7, [
          { offset: 0, color: color },
          { offset: 0.7, color: shadeColor(color, -20) },
          { offset: 1, color: shadeColor(color, -40) }
        ]),
        shadowBlur: 30,
        shadowColor: `${color}60`,
        borderWidth: 2,
        borderColor: '#fff'
      },
      label: {
        show: true,
        position: 'outside',
        distance: 8,
        fontSize: 12,
        fontWeight: 600,
        color: '#333',
        backgroundColor: 'rgba(255,255,255,0.9)',
        borderRadius: 4,
        padding: [4, 8],
        formatter: () => `${displayClassName}\n${displayCount}/${totalCount}`
      }
    }
    nodes.push(classNode)

    // 连接根节点到类别节点
    links.push({
      source: 'ARG_ROOT',
      target: classNodeId,
      value: sequences.length,
      lineStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 1, y2: 1,
          colorStops: [
            { offset: 0, color: '#3dccc7' },
            { offset: 1, color: color }
          ]
        },
        width: Math.min(6, 2 + Math.sqrt(sequences.length) / 4),
        opacity: 0.6,
        curveness: 0.2
      }
    })

    // 3. 创建序列节点（在类别节点周围分布）
    const displaySequences = sequences.slice(0, maxSeqPerCategory)
    const seqRadius = 120 // 序列节点距离类别节点的半径

    displaySequences.forEach((seq, seqIndex) => {
      const seqNodeId = `SEQ_${seq.id}`
      const seqProb = seq.predProb || 0

      // 序列节点在类别周围呈扇形分布
      const seqAngle = angle + (seqIndex - displaySequences.length / 2) / displaySequences.length * Math.PI * 0.6
      const seqDist = seqRadius + (seqIndex % 3) * 30 // 错落分布

      // 节点大小根据概率调整
      const seqNodeSize = 10 + seqProb * 25

      // 序列节点
      const seqNode = {
        id: seqNodeId,
        name: seq.id.length > 20 ? seq.id.substring(0, 17) + '...' : seq.id,
        fullName: seq.id,
        symbolSize: seqNodeSize,
        value: seqProb,
        category: categoryIndex,
        x: Math.cos(seqAngle) * (radiusStep + seqDist),
        y: Math.sin(seqAngle) * (radiusStep + seqDist),
        originalData: seq,
        itemStyle: {
          color: new echarts.graphic.RadialGradient(0.5, 0.5, 0.6, [
            { offset: 0, color: shadeColor(color, 20) },
            { offset: 1, color: color }
          ]),
          opacity: 0.7 + seqProb * 0.3,
          borderWidth: 1,
          borderColor: '#fff'
        },
        label: {
          show: false
        }
      }
      nodes.push(seqNode)

      // 连接类别节点到序列节点
      links.push({
        source: classNodeId,
        target: seqNodeId,
        lineStyle: {
          color: color,
          width: 0.5 + seqProb * 1.5,
          opacity: 0.2 + seqProb * 0.3
        }
      })
    })

    categories.push({
      name: displayClassName,
      itemStyle: { color }
    })

    categoryIndex++
  })

  // 计算合适的初始缩放（自动适应容器宽度）
  const nodeCount = nodes.length

  // 获取容器的实际宽度
  const containerWidth = networkChartRef.value?.clientWidth || 800

  // 计算节点分布范围
  let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity
  nodes.forEach(node => {
    if (node.x !== undefined) {
      minX = Math.min(minX, node.x)
      maxX = Math.max(maxX, node.x)
      minY = Math.min(minY, node.y)
      maxY = Math.max(maxY, node.y)
    }
  })

  // 计算网络图的实际宽度和高度
  const graphWidth = maxX - minX + 200 // 加上边距
  const graphHeight = maxY - minY + 200

  // 根据容器宽度计算缩放比例，使网络图完整显示
  const scaleByWidth = containerWidth / graphWidth
  const initialZoom = Math.min(1.0, Math.max(0.3, scaleByWidth * 0.9))

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: '#2a9d8f',
      borderWidth: 2,
      padding: [12, 16],
      textStyle: {
        color: '#333'
      },
      formatter: (params) => {
        if (params.dataType === 'node') {
          const data = params.data
          let html = `<div style="font-weight:600;color:#2a9d8f;margin-bottom:8px;font-size:14px;border-bottom:1px solid #eee;padding-bottom:6px;">${data.name || data.fullName || 'ARG'}</div>`

          if (data.id === 'ARG_ROOT') {
            html += `<div style="display:flex;align-items:center;gap:8px;">`
            html += `<span style="background:linear-gradient(135deg,#2a9d8f,#3dccc7);width:12px;height:12px;border-radius:50%;display:inline-block;"></span>`
            html += `<span>Total: <strong style="font-size:18px;color:#2a9d8f;">${data.value}</strong> sequences</span>`
            html += `</div>`
          } else if (data.id.startsWith('CLASS_')) {
            html += `<div style="display:flex;align-items:center;gap:8px;">`
            html += `<span style="background:${categories[data.category - 1]?.itemStyle?.color || '#2a9d8f'};width:12px;height:12px;border-radius:50%;display:inline-block;"></span>`
            html += `<span>Count: <strong>${data.value}</strong></span>`
            html += `</div>`
          } else if (data.originalData) {
            const seq = data.originalData
            html += `<div style="margin-bottom:6px;">`
            html += `<div style="color:#666;font-size:12px;margin-bottom:4px;">Sequence ID:</div>`
            html += `<div style="font-family:monospace;font-size:12px;word-break:break-all;">${seq.id}</div>`
            html += `</div>`
            html += `<div style="display:flex;justify-content:space-between;align-items:center;margin-top:8px;padding-top:8px;border-top:1px solid #eee;">`
            html += `<span>Probability:</span>`
            html += `<span style="background:linear-gradient(90deg,#2a9d8f,#3dccc7);color:#fff;padding:2px 10px;border-radius:10px;font-weight:600;">${(seq.predProb * 100).toFixed(1)}%</span>`
            html += `</div>`
            if (seq.argClass) {
              html += `<div style="margin-top:6px;"><span class="arg-tag" style="background:#e8f5f3;color:#2a9d8f;padding:2px 8px;border-radius:4px;font-size:11px;">${seq.argClass}</span></div>`
            }
            html += `<div style="margin-top:10px;color:#999;font-size:11px;text-align:center;">Click to BLAST →</div>`
          }
          return html
        }
        return ''
      }
    },
    legend: {
      show: true,
      orient: 'vertical',
      right: 15,
      top: 60,
      bottom: 20,
      width: 120,
      itemWidth: 14,
      itemHeight: 14,
      itemGap: 8,
      textStyle: {
        color: '#333',
        fontSize: 11
      },
      data: categories.map(c => c.name),
      formatter: (name) => {
        const cat = categories.find(c => c.name === name)
        return `{name|${name}}`
      },
      textStyle: {
        rich: {
          name: {
            fontSize: 11,
            color: '#333'
          }
        }
      }
    },
    animationDurationUpdate: 1500,
    animationEasingUpdate: 'quinticInOut',
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: nodes,
        links: links,
        categories: categories,
        roam: true,
        draggable: true,
        zoom: initialZoom,
        force: {
          repulsion: Math.max(800, 2000 - nodeCount * 2),
          gravity: 0.05,
          edgeLength: [60, 150],
          layoutAnimation: true,
          friction: 0.1,
          initLayout: 'none' // 使用预设位置
        },
        emphasis: {
          focus: 'adjacency',
          scale: true,
          lineStyle: {
            width: 3,
            opacity: 0.8
          },
          itemStyle: {
            shadowBlur: 40,
            shadowColor: 'rgba(42, 157, 143, 0.8)'
          }
        },
        lineStyle: {
          curveness: 0.15,
          opacity: 0.4
        },
        label: {
          show: false
        },
        symbol: 'circle',
        scaleLimit: {
          min: 0.1,
          max: 3
        },
        z: 10
      }
    ]
  }

  networkChartInstance.setOption(option, true)

  // 添加点击事件
  networkChartInstance.off('click')
  networkChartInstance.on('click', (params) => {
    if (params.dataType === 'node' && params.data.originalData) {
      const seq = params.data.originalData
      handleBlast(seq)
    }
  })

  console.log(`[Network Chart] Total nodes: ${nodes.length}, Categories: ${categoryCount}`)
}

// 颜色辅助函数 - 使颜色变亮或变暗
function shadeColor(color, percent) {
  const num = parseInt(color.replace('#', ''), 16)
  const amt = Math.round(2.55 * percent)
  const R = (num >> 16) + amt
  const G = (num >> 8 & 0x00FF) + amt
  const B = (num & 0x0000FF) + amt
  return '#' + (
    0x1000000 +
    (R < 255 ? (R < 1 ? 0 : R) : 255) * 0x10000 +
    (G < 255 ? (G < 1 ? 0 : G) : 255) * 0x100 +
    (B < 255 ? (B < 1 ? 0 : B) : 255)
  ).toString(16).slice(1)
}

// 网络图缩放控制
function zoomNetwork(scaleFactor) {
  if (!networkChartInstance) return
  const option = networkChartInstance.getOption()
  const series = option.series[0]
  const currentZoom = series.zoom || 1
  const newZoom = Math.max(0.05, Math.min(5, currentZoom * scaleFactor))
  
  networkChartInstance.setOption({
    series: [{
      zoom: newZoom
    }]
  })
}

// 重置网络图缩放
function resetNetworkZoom() {
  if (!networkChartInstance) return
  networkChartInstance.dispatchAction({
    type: 'restore'
  })
}

// 下载图表图片（使用青绿色主题背景）
function downloadChartImages() {
  try {
    ElMessage.info(t('visualization.messages.chartsGenerating'))
    
    let downloadCount = 0
    
    // 下载饼图
    if (pieChartInstance) {
      const pieUrl = pieChartInstance.getDataURL({
        type: 'png',
        pixelRatio: 2,
        backgroundColor: '#f8fdfc'
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
          backgroundColor: '#f8fdfc'
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

/* 青绿色主题 */
:deep(.el-card) {
  background: linear-gradient(135deg, #ffffff 0%, #f8fdfc 100%);
  border: 1px solid rgba(42, 157, 143, 0.15);
  box-shadow: 0 4px 20px rgba(42, 157, 143, 0.08);
  color: #1a3a36;
}

:deep(.el-card__header) {
  background: linear-gradient(135deg, rgba(42, 157, 143, 0.06) 0%, rgba(61, 204, 199, 0.1) 100%);
  border-bottom: 1px solid rgba(42, 157, 143, 0.2);
  color: #2a9d8f;
  font-weight: 600;
}

:deep(.el-descriptions__label) {
  color: #2a9d8f;
  background: rgba(42, 157, 143, 0.06);
  font-weight: 600;
}

:deep(.el-descriptions__content) {
  color: #1a3a36;
  background: #ffffff;
}

:deep(.el-table) {
  background: #ffffff;
  color: #1a3a36;
}

:deep(.el-table th.el-table__cell) {
  background: linear-gradient(135deg, rgba(42, 157, 143, 0.1) 0%, rgba(61, 204, 199, 0.15) 100%);
  color: #2a9d8f;
  border-bottom: 1px solid rgba(42, 157, 143, 0.25);
  font-weight: 600;
}

:deep(.el-table tr) {
  background: #ffffff;
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(42, 157, 143, 0.1);
  color: #1a3a36;
}

:deep(.el-tabs__item) {
  color: #606266;
  font-weight: 500;
}

:deep(.el-tabs__item.is-active) {
  color: #2a9d8f;
  font-weight: 600;
}

:deep(.el-tabs__active-bar) {
  background-color: #2a9d8f;
}

h3, h4, h5 {
  color: #2a9d8f;
  text-shadow: 0 2px 4px rgba(42, 157, 143, 0.12);
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
  color: #2a9d8f;
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
  background: rgba(42, 157, 143, 0.03);
  border: 1px solid rgba(42, 157, 143, 0.15);
  border-radius: 8px;
  transition: all 0.3s ease;
}

.download-item:hover {
  background: rgba(42, 157, 143, 0.06);
  border-color: rgba(42, 157, 143, 0.25);
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
  color: #2a9d8f;
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

/* 序列展开行样式 */
.sequence-expand-content {
  padding: 20px;
  background: linear-gradient(135deg, rgba(42, 157, 143, 0.03) 0%, rgba(61, 204, 199, 0.05) 100%);
  border-radius: 8px;
  border: 1px solid rgba(42, 157, 143, 0.1);
}

.sequence-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(42, 157, 143, 0.15);
}

.sequence-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sequence-info .el-icon {
  font-size: 20px;
  color: #2a9d8f;
}

.sequence-id {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
  font-weight: 600;
  color: #1a3a36;
}

.sequence-body {
  background: #fff;
  border-radius: 6px;
  padding: 16px;
  border: 1px solid rgba(42, 157, 143, 0.1);
}

.sequence-text {
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.8;
  color: #1a3a36;
  word-break: break-all;
  white-space: pre-wrap;
  max-height: 300px;
  overflow-y: auto;
}

/* 表格展开按钮样式 */
:deep(.el-table__expand-icon) {
  color: #2a9d8f;
  font-size: 14px;
}

:deep(.el-table__expand-icon:hover) {
  color: #238b7e;
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
  background: linear-gradient(135deg, #2a9d8f 0%, #3dccc7 100%);
  border: none;
  font-weight: 500;
  padding: 4px 10px;
  transition: all 0.3s ease;
}

.blast-btn:hover {
  background: linear-gradient(135deg, #238b7e 0%, #2a9d8f 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(42, 157, 143, 0.3);
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
  background-color: rgba(42, 157, 143, 0.12);
  border-color: #2a9d8f;
}

.arg-class-tag .info-icon {
  font-size: 12px;
  color: #909399;
  transition: color 0.2s ease;
}

.arg-class-tag:hover .info-icon {
  color: #2a9d8f;
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
  background-color: rgba(42, 157, 143, 0.05);
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
  background: linear-gradient(135deg, rgba(42, 157, 143, 0.04) 0%, rgba(61, 204, 199, 0.06) 100%);
  border-radius: 8px;
  border: 1px solid rgba(42, 157, 143, 0.12);
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
  border-top: 1px solid rgba(42, 157, 143, 0.12);
}

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-hover-color: #2a9d8f;
}

:deep(.el-pagination .el-pager li) {
  background: rgba(42, 157, 143, 0.04);
  color: #1a3a36;
  border: 1px solid rgba(42, 157, 143, 0.15);
  border-radius: 4px;
  margin: 0 2px;
}

:deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #2a9d8f 0%, #3dccc7 100%);
  color: #ffffff;
  border-color: #2a9d8f;
}

:deep(.el-pagination .el-pager li:hover:not(.is-active)) {
  color: #2a9d8f;
  background: rgba(42, 157, 143, 0.08);
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
  fill: rgba(42, 157, 143, 0.4) !important;
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

/* 统计概览卡片 - 科技感设计 */
.stats-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 28px;
}

.stat-card-teal {
  background: linear-gradient(135deg, #ffffff 0%, #f8fdfc 100%);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid rgba(42, 157, 143, 0.15);
  box-shadow: 0 4px 16px rgba(42, 157, 143, 0.08);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.stat-card-teal::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg, #2a9d8f 0%, #3dccc7 100%);
}

.stat-card-teal.arg::before {
  background: linear-gradient(180deg, #2a9d8f 0%, #4db8ab 100%);
}

.stat-card-teal.non-arg::before {
  background: linear-gradient(180deg, #a8b5b3 0%, #d1dcd9 100%);
}

.stat-card-teal.ratio::before {
  background: linear-gradient(180deg, #e9c46a 0%, #f4a261 100%);
}

.stat-card-teal:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(42, 157, 143, 0.15);
  border-color: rgba(42, 157, 143, 0.25);
}

.stat-icon-bg {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(42, 157, 143, 0.1) 0%, rgba(61, 204, 199, 0.15) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon-bg.arg {
  background: linear-gradient(135deg, rgba(42, 157, 143, 0.15) 0%, rgba(61, 204, 199, 0.2) 100%);
}

.stat-icon-bg.non-arg {
  background: linear-gradient(135deg, rgba(168, 181, 179, 0.15) 0%, rgba(209, 220, 217, 0.2) 100%);
}

.stat-icon-bg.ratio {
  background: linear-gradient(135deg, rgba(233, 196, 106, 0.15) 0%, rgba(244, 162, 97, 0.2) 100%);
}

.stat-icon-bg .el-icon {
  font-size: 26px;
  color: #2a9d8f;
}

.stat-icon-bg.arg .el-icon {
  color: #2a9d8f;
}

.stat-icon-bg.non-arg .el-icon {
  color: #a8b5b3;
}

.stat-icon-bg.ratio .el-icon {
  color: #e9c46a;
}

.stat-info {
  flex: 1;
}

.stat-value-teal {
  font-size: 28px;
  font-weight: 700;
  color: #2a9d8f;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-value-teal.arg {
  color: #2a9d8f;
}

.stat-value-teal.non-arg {
  color: #888;
}

.stat-value-teal.ratio {
  color: #e9a93f;
}

.stat-label-teal {
  font-size: 13px;
  color: #666;
  font-weight: 500;
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
  color: #2a9d8f;
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
  background: linear-gradient(135deg, rgba(42, 157, 143, 0.03) 0%, rgba(61, 204, 199, 0.05) 100%);
  padding: 20px;
  border-radius: 12px;
  border: 1px solid rgba(42, 157, 143, 0.15);
  box-shadow: 0 4px 16px rgba(42, 157, 143, 0.08);
  transition: all 0.3s ease;
}

.chart-container.enhanced {
  background: linear-gradient(135deg, #ffffff 0%, #f8fdfc 100%);
  border: 1px solid rgba(42, 157, 143, 0.12);
  box-shadow: 0 4px 20px rgba(42, 157, 143, 0.1);
}

.chart-container.enhanced:hover {
  transform: translateY(-2px);
  border-color: rgba(42, 157, 143, 0.25);
  box-shadow: 0 8px 28px rgba(42, 157, 143, 0.15);
}

.chart-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(42, 157, 143, 0.1);
}

.chart-container h4 {
  margin: 0 0 6px;
  font-size: 16px;
  color: #1a3a36;
  font-weight: 600;
}

.chart-container .chart-desc {
  color: #888;
  font-size: 12px;
  margin: 0;
  line-height: 1.5;
}

.chart {
  width: 100%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(248, 253, 252, 0.95) 100%);
  border-radius: 8px;
  border: 1px solid rgba(42, 157, 143, 0.12);
}

/* 网络图容器 */
.network-chart-container {
  grid-column: 1 / -1;
  min-height: 1050px;
}

.network-chart {
  height: 1000px !important;
  /* 优化触控体验 */
  touch-action: pan-x pan-y pinch-zoom;
  user-select: none;
  -webkit-user-select: none;
  /* 确保网络图有合适的背景 */
  background: linear-gradient(135deg, rgba(42, 157, 143, 0.02) 0%, rgba(61, 204, 199, 0.03) 100%);
  border-radius: 8px;
}

.network-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 1000px;
}

/* 网络图操作提示 */
.network-controls-hint {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.network-hint-alert {
  padding: 8px 12px;
}

.network-hint-alert :deep(.el-alert__title) {
  font-size: 13px;
  line-height: 1.6;
}

.hint-content {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
}

.hint-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #1a3a36;
  font-size: 13px;
}

.hint-item .el-icon {
  color: #2a9d8f;
  font-size: 14px;
}

/* 缩放控制按钮 */
.zoom-controls {
  display: flex;
  justify-content: center;
  margin-top: 4px;
}

.zoom-controls .el-button {
  padding: 6px 12px;
}

.zoom-controls .el-icon {
  font-size: 14px;
}
</style>

