import request from '@/utils/request'

/**
 * 获取 ARG 可视化数据（兼容：有落库则摘要+第一页，否则全量）
 */
export function getGenomeVisualization(taskId) {
  return request({
    url: `/visualization/genome/${taskId}`,
    method: 'get'
  })
}

/**
 * 获取摘要（总数、抗性数、非抗性数）
 */
export function getVisualizationSummary(taskId) {
  return request({
    url: `/visualization/genome/${taskId}/summary`,
    method: 'get'
  })
}

/**
 * 分页列表（支持筛选、搜索）
 */
export function getVisualizationResults(taskId, params = {}) {
  return request({
    url: `/visualization/genome/${taskId}/results`,
    method: 'get',
    params: {
      page: params.page ?? 1,
      pageSize: params.pageSize ?? 100,
      isArg: params.isArg ?? undefined,
      keyword: params.keyword ?? undefined
    }
  })
}

/**
 * 种类统计（第二张图）
 */
export function getClassSummary(taskId) {
  return request({
    url: `/visualization/genome/${taskId}/class-summary`,
    method: 'get'
  })
}

/**
 * 获取统计数据
 */
export function getStatistics(taskId) {
  return request({
    url: `/visualization/statistics/${taskId}`,
    method: 'get'
  })
}

/**
 * 导出可视化数据
 */
export function exportVisualizationData(taskId) {
  return request({
    url: `/visualization/export/${taskId}`,
    method: 'get',
    responseType: 'blob'
  })
}
