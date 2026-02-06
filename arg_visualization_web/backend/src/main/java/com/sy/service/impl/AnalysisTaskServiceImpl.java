package com.sy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sy.mapper.AllPredictionMapper;
import com.sy.mapper.AnalysisResultMapper;
import com.sy.mapper.AnalysisTaskMapper;
import com.sy.mapper.ClassSummaryMapper;
import com.sy.mapper.GenomeFileMapper;
import com.sy.pojo.AllPrediction;
import com.sy.pojo.AnalysisResult;
import com.sy.pojo.AnalysisTask;
import com.sy.pojo.ClassSummary;
import com.sy.pojo.GenomeFile;
import com.sy.service.AnalysisTaskService;
import com.sy.service.MagAnalysisService;
import com.sy.service.TaskQueueManager;
import com.sy.service.VisualizationService;
import com.sy.exception.TaskCancelledException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分析任务服务实现
 * 处理抗性基因（ARG）识别和分类任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisTaskServiceImpl implements AnalysisTaskService {

    private final AnalysisTaskMapper analysisTaskMapper;
    private final AnalysisResultMapper analysisResultMapper;
    private final GenomeFileMapper genomeFileMapper;
    private final AllPredictionMapper allPredictionMapper;
    private final ClassSummaryMapper classSummaryMapper;
    private final DockerServiceImpl dockerService;
    private final TaskQueueManager taskQueueManager;
    private final MagAnalysisService magAnalysisService;
    private final VisualizationService visualizationService;
    
    @Value("${analysis.output-dir:./outputs}")
    private String outputBaseDir;

    @Value("${file.upload.mag-dir:./uploads/mag}")
    private String magUploadDir;

    @Override
    public Map<String, Object> createTask(Long fileId, Long userId, Map<String, Object> params) {
        // 验证文件是否存在
        GenomeFile genomeFile = genomeFileMapper.selectById(fileId);
        if (genomeFile == null) {
            throw new RuntimeException("文件不存在");
        }
        if (!genomeFile.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该文件");
        }
        
        // 创建任务（任务名用英文格式，避免中文文件名写入 DB）
        AnalysisTask task = new AnalysisTask();
        task.setUserId(userId);
        task.setFileId(fileId);
        task.setTaskName("ARG - Task");
        task.setStatus("PENDING");
        task.setProgress(0);
        task.setCreatedAt(LocalDateTime.now());
        
        // 保存参数
        if (params != null && !params.isEmpty()) {
            task.setParameters(convertToJson(params));
        }
        
        // 保存到数据库
        analysisTaskMapper.insert(task);
        
        // 设置任务名与输出目录（英文：ARG - task_{id}）
        task.setTaskName("ARG - task_" + task.getTaskId());
        String outputDir = outputBaseDir + File.separator + "task_" + task.getTaskId();
        task.setOutputDir(outputDir);
        analysisTaskMapper.updateById(task);
        
        log.info("创建分析任务: taskId={}, fileId={}, userId={}", task.getTaskId(), fileId, userId);
        
        // 异步执行任务
        taskQueueManager.submitTask(task.getTaskId(), 
                () -> executeAnalysis(task.getTaskId(), genomeFile, params));
        
        return convertTaskToMap(task, genomeFile.getOriginalFilename());
    }

    @Override
    public List<Map<String, Object>> getUserTasks(Long userId, String status) {
        List<AnalysisTask> tasks;
        if (status != null && !status.isEmpty()) {
            tasks = analysisTaskMapper.findByUserIdAndStatus(userId, status);
        } else {
            tasks = analysisTaskMapper.findByUserId(userId);
        }
        
        return tasks.stream()
                .map(task -> convertTaskToMap(task, getTaskDisplayName(task)))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> searchTasks(Long userId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getUserTasks(userId, null);
        }
        
        List<AnalysisTask> tasks;
        try {
            Long taskId = Long.parseLong(keyword.trim());
            AnalysisTask task = analysisTaskMapper.selectById(taskId);
            if (task != null && task.getUserId().equals(userId)) {
                tasks = List.of(task);
            } else {
                tasks = analysisTaskMapper.searchTasks(userId, keyword.trim());
            }
        } catch (NumberFormatException e) {
            tasks = analysisTaskMapper.searchTasks(userId, keyword.trim());
        }
        
        return tasks.stream()
                .map(task -> convertTaskToMap(task, getTaskDisplayName(task)))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getTaskDetail(Long taskId, Long userId) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该任务");
        }
        
        return convertTaskToMap(task, getTaskDisplayName(task));
    }

    @Override
    public Map<String, Object> getTaskStatus(Long taskId, Long userId) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该任务");
        }
        
        Map<String, Object> status = new HashMap<>();
        status.put("taskId", task.getTaskId());
        status.put("status", task.getStatus());
        status.put("progress", task.getProgress());
        status.put("startedAt", task.getStartedAt() != null ? task.getStartedAt().toString() : null);
        status.put("completedAt", task.getCompletedAt() != null ? task.getCompletedAt().toString() : null);
        status.put("errorMessage", task.getErrorMessage());
        return status;
    }

    @Override
    public void cancelTask(Long taskId, Long userId) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该任务");
        }
        
        String status = task.getStatus();
        if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            throw new RuntimeException("任务已完成，无法取消");
        }
        
        // 终止 Docker 进程（若仍在跑）
        dockerService.cancelAnalysis(taskId);
        // 中断执行线程（含落库阶段），否则落库完成后会覆盖为 COMPLETED
        taskQueueManager.cancelTask(taskId);

        task.setStatus("CANCELLED");
        task.setCompletedAt(LocalDateTime.now());
        analysisTaskMapper.updateById(task);

        log.info("任务已取消: taskId={}", taskId);
    }

    @Override
    public void deleteTask(Long taskId, Long userId) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除该任务");
        }
        log.info("用户删除任务: taskId={}, userId={}", taskId, userId);
        deleteTaskAndRelatedData(taskId);
    }

    @Override
    public void deleteTaskAndRelatedData(Long taskId) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("deleteTaskAndRelatedData: 任务不存在, taskId={}", taskId);
            return;
        }
        log.info("级联删除任务及关联数据: taskId={}", taskId);

        // 1. 删除 all_predictions
        int deletedPredictions = allPredictionMapper.delete(
                new LambdaQueryWrapper<AllPrediction>().eq(AllPrediction::getTaskId, taskId));
        log.info("删除任务 {} 的 all_predictions: {} 条", taskId, deletedPredictions);

        // 2. 删除 class_summary
        int deletedSummary = classSummaryMapper.delete(
                new LambdaQueryWrapper<ClassSummary>().eq(ClassSummary::getTaskId, taskId));
        log.info("删除任务 {} 的 class_summary: {} 条", taskId, deletedSummary);

        // 3. 删除 analysis_results（表无 FK 时需显式删；有 CASCADE 时删除任务时会自动删，显式删更稳妥）
        int deletedResults = analysisResultMapper.delete(
                new LambdaQueryWrapper<AnalysisResult>().eq(AnalysisResult::getTaskId, taskId));
        log.info("删除任务 {} 的 analysis_results: {} 条", taskId, deletedResults);

        // 4. 删除任务输出目录
        if (task.getOutputDir() != null) {
            try {
                Path outputPath = Paths.get(task.getOutputDir());
                if (Files.exists(outputPath)) {
                    Files.walk(outputPath)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                    log.info("删除任务输出目录: {}", task.getOutputDir());
                }
            } catch (Exception e) {
                log.error("删除任务输出目录失败: {}", task.getOutputDir(), e);
            }
        }

        // 5. 删除任务记录
        analysisTaskMapper.deleteById(taskId);
        log.info("任务及关联数据删除完成: taskId={}", taskId);
    }

    /** 批量删除时 IN 子句每批 task_id 数量，避免 SQL 过长 */
    private static final int BATCH_DELETE_TASK_IDS_SIZE = 200;

    @Override
    public void deleteTasksAndRelatedDataBatch(List<AnalysisTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        List<Long> taskIds = tasks.stream().map(AnalysisTask::getTaskId).distinct().collect(Collectors.toList());
        log.info("批量删除任务及关联数据: 任务数={}", taskIds.size());

        // 1. 按批批量删除 all_predictions、class_summary、analysis_results（减少 round-trip）
        for (int i = 0; i < taskIds.size(); i += BATCH_DELETE_TASK_IDS_SIZE) {
            int to = Math.min(i + BATCH_DELETE_TASK_IDS_SIZE, taskIds.size());
            List<Long> chunk = taskIds.subList(i, to);
            int p = allPredictionMapper.delete(new LambdaQueryWrapper<AllPrediction>().in(AllPrediction::getTaskId, chunk));
            int s = classSummaryMapper.delete(new LambdaQueryWrapper<ClassSummary>().in(ClassSummary::getTaskId, chunk));
            int r = analysisResultMapper.delete(new LambdaQueryWrapper<AnalysisResult>().in(AnalysisResult::getTaskId, chunk));
            log.debug("批量删除一批: taskIds={}, all_predictions={}, class_summary={}, analysis_results={}", chunk.size(), p, s, r);
        }

        // 2. 按任务删除输出目录和任务记录
        for (AnalysisTask task : tasks) {
            Long taskId = task.getTaskId();
            if (task.getOutputDir() != null) {
                try {
                    Path path = Paths.get(task.getOutputDir());
                    if (Files.exists(path)) {
                        Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                        log.debug("删除任务输出目录: {}", task.getOutputDir());
                    }
                } catch (Exception e) {
                    log.error("删除任务输出目录失败: taskId={}", taskId, e);
                }
            }
            analysisTaskMapper.deleteById(taskId);
        }
        log.info("批量删除任务及关联数据完成: 任务数={}", taskIds.size());
    }

    @Override
    public Map<String, Object> getTaskResult(Long taskId, Long userId) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该任务");
        }
        
        if (!"COMPLETED".equals(task.getStatus())) {
            throw new RuntimeException("任务未完成");
        }
        
        GenomeFile fileInfo = genomeFileMapper.selectById(task.getFileId());
        
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("taskId", task.getTaskId());
        resultData.put("fileName", fileInfo != null ? fileInfo.getOriginalFilename() : "未知文件");
        resultData.put("status", task.getStatus());
        resultData.put("createdAt", task.getCreatedAt());
        resultData.put("completedAt", task.getCompletedAt());
        
        // 计算运行时长
        if (task.getStartedAt() != null && task.getCompletedAt() != null) {
            long seconds = java.time.Duration.between(task.getStartedAt(), task.getCompletedAt()).getSeconds();
            resultData.put("duration", formatDuration(seconds));
        } else {
            resultData.put("duration", "-");
        }
        
        resultData.put("argCount", task.getProphageCount());
        
        return resultData;
    }

    /**
     * 执行分析任务
     */
    private void executeAnalysis(Long taskId, GenomeFile fileInfo, Map<String, Object> params) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            log.error("任务不存在: taskId={}", taskId);
            return;
        }
        
        try {
            // 更新任务状态
            task.setStatus("RUNNING");
            task.setStartedAt(LocalDateTime.now());
            task.setProgress(10);
            analysisTaskMapper.updateById(task);
            
            log.info("开始执行分析任务: taskId={}", taskId);
            
            String inputFilePath = fileInfo.getFilePath();
            String outputDir = task.getOutputDir();
            
            task.setProgress(20);
            analysisTaskMapper.updateById(task);
            
            // 执行抗性基因检测
            Map<String, Object> result = dockerService.runArgDetection(taskId, inputFilePath, outputDir, params);
            
            // 先落库再标记完成，避免用户点击「查看结果」时拿到不完整数据
            task.setProgress(90);
            analysisTaskMapper.updateById(task);
            log.info("ARG 模型执行完成，开始持久化结果: taskId={}", taskId);
            
            try {
                visualizationService.persistTaskResultsToDb(taskId);
            } catch (TaskCancelledException e) {
                log.info("任务已取消，停止落库: taskId={}", taskId);
                return;
            } catch (Exception ex) {
                log.warn("落库失败，使用结果中的数量: taskId={}", taskId, ex);
                List<Map<String, Object>> argResults = (List<Map<String, Object>>) result.get("argResults");
                if (argResults != null) {
                    long argCount = argResults.stream().filter(r -> Boolean.TRUE.equals(r.get("isArg"))).count();
                    task.setProphageCount((int) argCount);
                    analysisTaskMapper.updateById(task);
                }
            }

            // 若已被用户取消，不再覆盖为 COMPLETED
            task = analysisTaskMapper.selectById(taskId);
            if (task != null && "CANCELLED".equals(task.getStatus())) {
                log.info("任务已取消，不更新为完成: taskId={}", taskId);
                return;
            }
            // 持久化完成后再标记任务为 COMPLETED，前端此时才能看到完整数据
            task.setStatus("COMPLETED");
            task.setProgress(100);
            task.setCompletedAt(LocalDateTime.now());
            analysisTaskMapper.updateById(task);
            log.info("分析任务完成（已落库）: taskId={}", taskId);

        } catch (TaskCancelledException e) {
            log.info("任务已取消: taskId={}", taskId);
        } catch (Exception e) {
            log.error("分析任务失败: taskId={}", taskId, e);
            task = analysisTaskMapper.selectById(taskId);
            if (task != null && !"CANCELLED".equals(task.getStatus())) {
                task.setStatus("FAILED");
                task.setErrorMessage(e.getMessage());
                task.setCompletedAt(LocalDateTime.now());
                analysisTaskMapper.updateById(task);
            }
        }
    }

    /**
     * 将任务转换为Map
     */
    private Map<String, Object> convertTaskToMap(AnalysisTask task, String fileName) {
        Map<String, Object> map = new HashMap<>();
        map.put("taskId", task.getTaskId());
        map.put("userId", task.getUserId());
        map.put("fileId", task.getFileId());
        map.put("fileName", fileName);
        map.put("taskName", task.getTaskName());
        map.put("status", task.getStatus());
        map.put("progress", task.getProgress());
        map.put("createdAt", task.getCreatedAt() != null ? task.getCreatedAt().toString() : null);
        map.put("startedAt", task.getStartedAt() != null ? task.getStartedAt().toString() : null);
        map.put("completedAt", task.getCompletedAt() != null ? task.getCompletedAt().toString() : null);
        map.put("errorMessage", task.getErrorMessage());
        map.put("argCount", task.getProphageCount());
        return map;
    }

    /**
     * 格式化时长
     */
    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("小时");
        if (minutes > 0) sb.append(minutes).append("分");
        if (secs > 0 || sb.length() == 0) sb.append(secs).append("秒");
        
        return sb.toString();
    }

    /**
     * 获取任务的显示文件名
     * 对于普通任务，返回关联文件的原始文件名
     * 对于 MAG 任务，返回 MAG 目录名或从任务名称中提取
     */
    private String getTaskDisplayName(AnalysisTask task) {
        // 如果有关联文件，优先使用文件名
        if (task.getFileId() != null) {
            GenomeFile file = genomeFileMapper.selectById(task.getFileId());
            if (file != null) {
                return file.getOriginalFilename();
            }
        }
        
        // MAG 任务：尝试从 magDirPath 提取目录名
        if ("MAG".equals(task.getTaskType()) && task.getMagDirPath() != null) {
            Path magDir = Paths.get(task.getMagDirPath());
            String dirName = magDir.getFileName().toString();
            // 如果目录名是自动生成的（如 mag_1_20260109...），则从任务名称提取
            if (dirName.startsWith("mag_") && task.getTaskName() != null) {
                // 任务名称格式: "MAG 抗性基因检测 - xxx"
                String taskName = task.getTaskName();
                if (taskName.contains(" - ")) {
                    return taskName.substring(taskName.lastIndexOf(" - ") + 3);
                }
            }
            return dirName;
        }
        
        // 从任务名称提取
        if (task.getTaskName() != null && task.getTaskName().contains(" - ")) {
            return task.getTaskName().substring(task.getTaskName().lastIndexOf(" - ") + 3);
        }
        
        return "Unknown";
    }

    /**
     * 将Map转换为JSON字符串
     */
    private String convertToJson(Map<String, Object> map) {
        try {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                Object value = entry.getValue();
                if (value instanceof String) {
                    sb.append("\"").append(value).append("\"");
                } else {
                    sb.append(value);
                }
                first = false;
            }
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 创建 MAG 分析任务
     * @param magDirPath MAG 文件夹路径
     * @param userId 用户ID
     * @param magName MAG 名称
     * @param params 分析参数
     * @return 任务信息
     */
    public Map<String, Object> createMagTask(String magDirPath, Long userId, String magName, Map<String, Object> params) {
        // 统计 MAG 文件数量
        int fileCount = 0;
        try {
            Path magDir = Paths.get(magDirPath);
            if (Files.exists(magDir) && Files.isDirectory(magDir)) {
                fileCount = (int) Files.list(magDir)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".fa") || name.endsWith(".fasta") || name.endsWith(".fna");
                    })
                    .count();
            }
        } catch (Exception e) {
            log.warn("统计 MAG 文件数量失败", e);
        }
        
        // 创建任务（任务名用英文格式，避免中文 mag 名写入 DB）
        AnalysisTask task = new AnalysisTask();
        task.setUserId(userId);
        task.setFileId(null);  // MAG 任务没有对应的单个文件
        task.setTaskType("MAG");  // 设置任务类型
        task.setMagDirPath(magDirPath);  // 设置 MAG 目录路径
        task.setMagFileCount(fileCount);  // 设置文件数量
        task.setTaskName("ARG - MAG");
        task.setStatus("PENDING");
        task.setProgress(0);
        task.setCreatedAt(LocalDateTime.now());
        
        // 保存参数（包含 MAG 路径和类型标识）
        Map<String, Object> taskParams = new HashMap<>();
        if (params != null) {
            taskParams.putAll(params);
        }
        taskParams.put("analysisType", "mag");
        taskParams.put("magDirPath", magDirPath);
        taskParams.put("magName", magName);
        task.setParameters(convertToJson(taskParams));
        
        // 保存到数据库
        analysisTaskMapper.insert(task);
        
        // 设置任务名与输出目录（英文：ARG - mag_{id}）
        task.setTaskName("ARG - mag_" + task.getTaskId());
        String outputDir = outputBaseDir + File.separator + "task_" + task.getTaskId();
        task.setOutputDir(outputDir);
        analysisTaskMapper.updateById(task);
        
        log.info("创建 MAG 分析任务: taskId={}, magDir={}, userId={}", task.getTaskId(), magDirPath, userId);
        
        // 异步执行 MAG 分析任务
        taskQueueManager.submitTask(task.getTaskId(), 
                () -> executeMagAnalysis(task.getTaskId(), magDirPath, params));
        
        return convertTaskToMap(task, magName);
    }

    /**
     * 执行 MAG 分析任务
     */
    private void executeMagAnalysis(Long taskId, String magDirPath, Map<String, Object> params) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            log.error("任务不存在: taskId={}", taskId);
            return;
        }
        
        try {
            // 更新任务状态
            task.setStatus("RUNNING");
            task.setStartedAt(LocalDateTime.now());
            task.setProgress(0);
            analysisTaskMapper.updateById(task);
            
            log.info("开始执行 MAG 分析任务: taskId={}", taskId);
            
            Path magDir = Paths.get(magDirPath);
            Path outputDir = Paths.get(task.getOutputDir());
            
            // 执行 MAG 分析（包含 Prodigal 预处理 + ARG 分析）
            Map<String, Object> result = magAnalysisService.analyzeMag(taskId, magDir, outputDir, params);
            
            // 先落库再标记完成，避免用户点击「查看结果」时拿到不完整数据
            task.setProgress(90);
            analysisTaskMapper.updateById(task);
            log.info("MAG 分析执行完成，开始持久化结果: taskId={}", taskId);
            
            try {
                visualizationService.persistTaskResultsToDb(taskId);
            } catch (TaskCancelledException e) {
                log.info("MAG 任务已取消，停止落库: taskId={}", taskId);
                return;
            } catch (Exception ex) {
                log.warn("落库失败，使用结果中的数量: taskId={}", taskId, ex);
                Object argCountObj = result.get("argCount");
                if (argCountObj != null) {
                    task.setProphageCount(((Number) argCountObj).intValue());
                    analysisTaskMapper.updateById(task);
                }
            }

            task = analysisTaskMapper.selectById(taskId);
            if (task != null && "CANCELLED".equals(task.getStatus())) {
                log.info("MAG 任务已取消，不更新为完成: taskId={}", taskId);
                return;
            }
            task.setStatus("COMPLETED");
            task.setProgress(100);
            task.setCompletedAt(LocalDateTime.now());
            analysisTaskMapper.updateById(task);
            log.info("MAG 分析任务完成（已落库）: taskId={}", taskId);

        } catch (TaskCancelledException e) {
            log.info("MAG 任务已取消: taskId={}", taskId);
        } catch (Exception e) {
            log.error("MAG 分析任务失败: taskId={}", taskId, e);
            task = analysisTaskMapper.selectById(taskId);
            if (task != null && !"CANCELLED".equals(task.getStatus())) {
                task.setStatus("FAILED");
                task.setErrorMessage(e.getMessage());
                task.setCompletedAt(LocalDateTime.now());
                analysisTaskMapper.updateById(task);
            }
        }
    }
}
