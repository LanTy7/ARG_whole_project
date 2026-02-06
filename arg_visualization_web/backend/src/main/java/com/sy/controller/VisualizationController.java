package com.sy.controller;

import com.sy.service.VisualizationService;
import com.sy.util.JwtUtil;
import com.sy.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 可视化数据控制器
 * 提供抗性基因（ARG）识别结果的可视化数据
 */
@Slf4j
@RestController
@RequestMapping("/api/visualization")
@RequiredArgsConstructor
public class VisualizationController {

    private final VisualizationService visualizationService;
    private final JwtUtil jwtUtil;

    /**
     * 获取 ARG 可视化数据（兼容：有落库则摘要+第一页，否则读文件）
     */
    @GetMapping("/genome/{taskId}")
    public Result<Map<String, Object>> getGenomeVisualization(
            @PathVariable Long taskId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
            Map<String, Object> data = visualizationService.getGenomeVisualization(taskId, userId);
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取可视化数据失败", e);
            return Result.error("获取可视化数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取摘要（总数、抗性数、非抗性数）
     */
    @GetMapping("/genome/{taskId}/summary")
    public Result<Map<String, Object>> getSummary(
            @PathVariable Long taskId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
            return Result.success(visualizationService.getSummary(taskId, userId));
        } catch (Exception e) {
            log.error("获取摘要失败", e);
            return Result.error("获取摘要失败: " + e.getMessage());
        }
    }

    /**
     * 分页列表（支持筛选 isArg、搜索 keyword）
     */
    @GetMapping("/genome/{taskId}/results")
    public Result<Map<String, Object>> getResultsPage(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(required = false) Boolean isArg,
            @RequestParam(required = false) String keyword,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
            Map<String, Object> data = visualizationService.getResultsPage(taskId, userId, page, pageSize, isArg, keyword);
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取分页结果失败", e);
            return Result.error("获取分页结果失败: " + e.getMessage());
        }
    }

    /**
     * 种类统计（第二张图）
     */
    @GetMapping("/genome/{taskId}/class-summary")
    public Result<Map<String, Object>> getClassSummary(
            @PathVariable Long taskId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
            return Result.success(visualizationService.getClassSummary(taskId, userId));
        } catch (Exception e) {
            log.error("获取种类统计失败", e);
            return Result.error("获取种类统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取统计图表数据
     */
    @GetMapping("/statistics/{taskId}")
    public Result<Map<String, Object>> getStatistics(
            @PathVariable Long taskId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
            Map<String, Object> statistics = visualizationService.getStatistics(taskId, userId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            return Result.error("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 导出可视化数据
     */
    @GetMapping("/export/{taskId}")
    public Result<Map<String, Object>> exportVisualizationData(
            @PathVariable Long taskId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
            Map<String, Object> data = visualizationService.exportVisualizationData(taskId, userId);
            return Result.success(data);
        } catch (Exception e) {
            log.error("导出可视化数据失败", e);
            return Result.error("导出数据失败: " + e.getMessage());
        }
    }
}
