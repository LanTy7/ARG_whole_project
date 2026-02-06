package com.sy.service;

import com.sy.pojo.AnalysisTask;

import java.util.List;
import java.util.Map;

/**
 * 分析任务服务接口
 */
public interface AnalysisTaskService {
    
    /**
     * 创建分析任务
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param params 分析参数
     * @return 任务信息
     */
    Map<String, Object> createTask(Long fileId, Long userId, Map<String, Object> params);
    
    /**
     * 获取用户的任务列表
     * @param userId 用户ID
     * @param status 任务状态（可选）
     * @return 任务列表
     */
    List<Map<String, Object>> getUserTasks(Long userId, String status);
    
    /**
     * 获取任务详情
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 任务详情
     */
    Map<String, Object> getTaskDetail(Long taskId, Long userId);
    
    /**
     * 获取任务状态
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 任务状态
     */
    Map<String, Object> getTaskStatus(Long taskId, Long userId);
    
    /**
     * 取消任务
     * @param taskId 任务ID
     * @param userId 用户ID
     */
    void cancelTask(Long taskId, Long userId);
    
    /**
     * 删除任务（带权限校验，用户只能删自己的任务）
     * @param taskId 任务ID
     * @param userId 用户ID
     */
    void deleteTask(Long taskId, Long userId);

    /**
     * 级联删除任务及其关联数据（不校验权限，供删除文件/用户等场景调用）
     * 删除：all_predictions、class_summary、analysis_results、任务输出目录、analysis_tasks 记录
     * @param taskId 任务ID
     */
    void deleteTaskAndRelatedData(Long taskId);

    /**
     * 批量级联删除多个任务及其关联数据（先按 task_id IN 批量删表，再按任务删目录和记录，减少 round-trip）
     * @param tasks 任务列表，不能为 null
     */
    void deleteTasksAndRelatedDataBatch(List<AnalysisTask> tasks);

    /**
     * 获取任务结果
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 分析结果
     */
    Map<String, Object> getTaskResult(Long taskId, Long userId);
    
    /**
     * 搜索任务（根据任务ID、文件ID或文件名）
     * @param userId 用户ID
     * @param keyword 搜索关键字
     * @return 任务列表
     */
    List<Map<String, Object>> searchTasks(Long userId, String keyword);
}

