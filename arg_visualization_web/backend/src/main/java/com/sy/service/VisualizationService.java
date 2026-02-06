package com.sy.service;

import java.util.Map;

/**
 * 可视化服务接口
 */
public interface VisualizationService {

    /**
     * 任务完成后将 TSV 结果落库（仅调用一次）
     * @param taskId 任务ID
     */
    void persistTaskResultsToDb(Long taskId);

    /**
     * 获取摘要（总数、抗性数、非抗性数），仅查 analysis_tasks
     */
    Map<String, Object> getSummary(Long taskId, Long userId);

    /**
     * 分页列表（支持筛选 isArg、搜索 keyword），查 all_predictions
     */
    Map<String, Object> getResultsPage(Long taskId, Long userId, int page, int pageSize, Boolean isArg, String keyword);

    /**
     * 种类统计（第二张图），查 class_summary
     */
    Map<String, Object> getClassSummary(Long taskId, Long userId);
    
    /**
     * 获取基因组可视化数据（兼容旧接口：优先从 DB 返回摘要+第一页，无 DB 时回退读文件）
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 可视化数据
     */
    Map<String, Object> getGenomeVisualization(Long taskId, Long userId);
    
    /**
     * 获取原噬菌体区域详情
     */
    Map<String, Object> getProphageDetail(Long taskId, Long regionId, Long userId);
    
    /**
     * 获取统计数据（从 analysis_tasks + class_summary）
     */
    Map<String, Object> getStatistics(Long taskId, Long userId);
    
    /**
     * 导出可视化数据
     */
    Map<String, Object> exportVisualizationData(Long taskId, Long userId);
}

