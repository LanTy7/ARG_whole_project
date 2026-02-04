package com.sy;

import com.sy.mapper.GenomeFileMapper;
import com.sy.pojo.GenomeFile;
import com.sy.service.AnalysisTaskService;
import com.sy.service.GenomeFileService;
import com.sy.service.TaskQueueManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 并发压力测试
 * 
 * 测试场景：
 * 1. 1MB目录（1000个文件）- 模拟1000人同时使用
 * 2. 100KB目录（10000个文件）- 模拟10000人同时使用
 * 
 * 使用方法：
 * 1. 修改下面的 TEST_DIR_1MB 和 TEST_DIR_100KB 路径
 * 2. 运行测试：mvn test -Dtest=ConcurrentLoadTest#testConcurrent1000
 */
@Slf4j
@SpringBootTest
public class ConcurrentLoadTest {

    @Autowired
    private GenomeFileService genomeFileService;

    @Autowired
    private AnalysisTaskService analysisTaskService;

    @Autowired
    private GenomeFileMapper genomeFileMapper;

    @Autowired
    private TaskQueueManager taskQueueManager;

    // ============= 配置区域 - 请根据实际情况修改 =============
    
    /** 1MB目录路径（1000个faa文件） */
    private static final String TEST_DIR_1MB = "/home/zhaoshuy/argnew/test/ncbi_dataset/data/merged_proteins/1MB";
    
    /** 100KB目录路径（10000个faa文件） */
    private static final String TEST_DIR_100KB = "/home/zhaoshuy/argnew/test/ncbi_dataset/data/merged_proteins/100KB";
    
    /** 测试用户ID（使用一个已存在的用户） */
    private static final Long TEST_USER_ID = 3L;
    
    /** 结果输出目录 */
    private static final String OUTPUT_DIR = "/home/zhaoshuy/argnew/test/output";
    
    // ============= 配置区域结束 =============

    /**
     * 测试1000并发（1MB目录）
     */
    @Test
    public void testConcurrent1000() throws Exception {
        runConcurrentTest(TEST_DIR_1MB, "1000_concurrent", 100);  // 并发线程数100
    }

    /**
     * 测试10000并发（100KB目录）
     */
    @Test
    public void testConcurrent10000() throws Exception {
        runConcurrentTest(TEST_DIR_100KB, "10000_concurrent", 200);  // 并发线程数200
    }

    /**
     * 小规模测试（调试用）
     */
    @Test
    public void testSmallScale() throws Exception {
        // 使用1MB目录，但只取前10个文件
        runConcurrentTestWithLimit(TEST_DIR_100KB, "small_scale_test", 5, 20);
    }

    /**
     * 运行并发测试
     * 
     * @param testDir 测试文件目录
     * @param testName 测试名称
     * @param concurrentThreads 并发线程数
     */
    private void runConcurrentTest(String testDir, String testName, int concurrentThreads) throws Exception {
        runConcurrentTestWithLimit(testDir, testName, concurrentThreads, -1);
    }

    /**
     * 运行并发测试（可限制文件数量）
     */
    private void runConcurrentTestWithLimit(String testDir, String testName, int concurrentThreads, int maxFiles) throws Exception {
        log.info("========================================");
        log.info("开始并发压力测试: {}", testName);
        log.info("测试目录: {}", testDir);
        log.info("并发线程数: {}", concurrentThreads);
        log.info("========================================");

        // 1. 扫描目录获取所有faa文件
        List<File> faaFiles = scanFaaFiles(testDir);
        if (faaFiles.isEmpty()) {
            log.error("目录中没有找到faa文件: {}", testDir);
            return;
        }

        // 限制文件数量（用于调试）
        if (maxFiles > 0 && faaFiles.size() > maxFiles) {
            faaFiles = faaFiles.subList(0, maxFiles);
        }

        int totalFiles = faaFiles.size();
        log.info("找到 {} 个faa文件", totalFiles);

        // 2. 创建统计对象
        TestStatistics stats = new TestStatistics(testName, totalFiles, concurrentThreads);

        // 3. 创建线程池
        ExecutorService uploadExecutor = Executors.newFixedThreadPool(concurrentThreads);
        ExecutorService analysisExecutor = Executors.newFixedThreadPool(concurrentThreads);

        // 4. 阶段1：并发上传文件
        log.info("===== 阶段1: 并发上传文件 =====");
        stats.startUploadPhase();

        List<Future<Long>> uploadFutures = new ArrayList<>();
        AtomicInteger uploadCount = new AtomicInteger(0);
        AtomicInteger uploadFailCount = new AtomicInteger(0);

        for (File file : faaFiles) {
            Future<Long> future = uploadExecutor.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    
                    // 上传文件
                    Long fileId = uploadFile(file);
                    
                    long elapsed = System.currentTimeMillis() - startTime;
                    stats.recordUpload(elapsed, true);
                    
                    int count = uploadCount.incrementAndGet();
                    if (count % 100 == 0) {
                        log.info("上传进度: {}/{}", count, totalFiles);
                    }
                    
                    return fileId;
                } catch (Exception e) {
                    uploadFailCount.incrementAndGet();
                    stats.recordUpload(0, false);
                    log.error("上传失败: {}", file.getName(), e);
                    return null;
                }
            });
            uploadFutures.add(future);
        }

        // 等待所有上传完成，收集fileId
        List<Long> fileIds = new ArrayList<>();
        for (Future<Long> future : uploadFutures) {
            try {
                Long fileId = future.get(5, TimeUnit.MINUTES);
                if (fileId != null) {
                    fileIds.add(fileId);
                }
            } catch (Exception e) {
                log.error("获取上传结果失败", e);
            }
        }

        stats.endUploadPhase();
        log.info("上传完成: 成功 {}, 失败 {}", fileIds.size(), uploadFailCount.get());

        // 5. 阶段2：并发创建分析任务
        log.info("===== 阶段2: 并发创建分析任务 =====");
        stats.startAnalysisPhase();

        List<Future<Long>> analysisFutures = new ArrayList<>();
        AtomicInteger analysisCount = new AtomicInteger(0);
        AtomicInteger analysisFailCount = new AtomicInteger(0);
        List<Long> taskIds = Collections.synchronizedList(new ArrayList<>());

        for (Long fileId : fileIds) {
            Future<Long> future = analysisExecutor.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    
                    // 创建分析任务
                    Map<String, Object> result = analysisTaskService.createTask(fileId, TEST_USER_ID, new HashMap<>());
                    Long taskId = Long.valueOf(result.get("taskId").toString());
                    
                    long elapsed = System.currentTimeMillis() - startTime;
                    stats.recordAnalysisSubmit(elapsed, true);
                    
                    int count = analysisCount.incrementAndGet();
                    if (count % 100 == 0) {
                        log.info("任务提交进度: {}/{}", count, fileIds.size());
                    }
                    
                    taskIds.add(taskId);
                    return taskId;
                } catch (Exception e) {
                    analysisFailCount.incrementAndGet();
                    stats.recordAnalysisSubmit(0, false);
                    log.error("创建任务失败: fileId={}", fileId, e);
                    return null;
                }
            });
            analysisFutures.add(future);
        }

        // 等待所有任务提交完成
        for (Future<Long> future : analysisFutures) {
            try {
                future.get(5, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.error("获取任务结果失败", e);
            }
        }

        stats.endAnalysisPhase();
        log.info("任务提交完成: 成功 {}, 失败 {}", taskIds.size(), analysisFailCount.get());

        // 6. 阶段3：监控任务完成情况
        log.info("===== 阶段3: 监控任务执行 =====");
        stats.startExecutionPhase();

        monitorTaskCompletion(taskIds, stats);

        stats.endExecutionPhase();

        // 7. 关闭线程池
        uploadExecutor.shutdown();
        analysisExecutor.shutdown();

        // 8. 生成报告
        String report = stats.generateReport();
        log.info("\n{}", report);

        // 保存报告到文件
        saveReport(testName, report, stats);

        log.info("========================================");
        log.info("测试完成: {}", testName);
        log.info("========================================");
    }

    /**
     * 扫描目录获取所有faa文件
     */
    private List<File> scanFaaFiles(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return Collections.emptyList();
        }

        File[] files = dir.listFiles((d, name) -> 
            name.toLowerCase().endsWith(".faa") || 
            name.toLowerCase().endsWith(".fasta") ||
            name.toLowerCase().endsWith(".fa"));

        return files != null ? Arrays.asList(files) : Collections.emptyList();
    }

    /**
     * 上传单个文件
     */
    private Long uploadFile(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                file.getName(),
                "text/plain",
                fis
            );

            Map<String, Object> options = new HashMap<>();
            options.put("fileType", "fasta");
            options.put("description", "压力测试文件");
            options.put("isPublic", false);

            Map<String, Object> result = genomeFileService.uploadGenomeFile(multipartFile, TEST_USER_ID, options);
            return Long.valueOf(result.get("fileId").toString());
        }
    }

    /**
     * 监控任务完成情况
     */
    private void monitorTaskCompletion(List<Long> taskIds, TestStatistics stats) {
        if (taskIds.isEmpty()) return;

        int totalTasks = taskIds.size();
        Set<Long> completedTasks = ConcurrentHashMap.newKeySet();
        Set<Long> failedTasks = ConcurrentHashMap.newKeySet();

        long startTime = System.currentTimeMillis();
        long timeout = 30 * 60 * 1000; // 30分钟超时

        while (completedTasks.size() + failedTasks.size() < totalTasks) {
            // 检查超时
            if (System.currentTimeMillis() - startTime > timeout) {
                log.warn("监控超时，停止等待");
                break;
            }

            // 检查每个任务状态
            for (Long taskId : taskIds) {
                if (completedTasks.contains(taskId) || failedTasks.contains(taskId)) {
                    continue;
                }

                try {
                    Map<String, Object> status = analysisTaskService.getTaskStatus(taskId, TEST_USER_ID);
                    String taskStatus = (String) status.get("status");

                    if ("COMPLETED".equals(taskStatus)) {
                        completedTasks.add(taskId);
                        stats.recordTaskComplete(true);
                    } else if ("FAILED".equals(taskStatus) || "CANCELLED".equals(taskStatus)) {
                        failedTasks.add(taskId);
                        stats.recordTaskComplete(false);
                    }
                } catch (Exception e) {
                    // 忽略查询错误
                }
            }

            int completed = completedTasks.size() + failedTasks.size();
            if (completed % 50 == 0 || completed == totalTasks) {
                log.info("任务执行进度: {}/{} (完成: {}, 失败: {})", 
                    completed, totalTasks, completedTasks.size(), failedTasks.size());
            }

            // 等待一段时间再检查
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        stats.setCompletedTasks(completedTasks.size());
        stats.setFailedTasks(failedTasks.size());
    }

    /**
     * 保存报告到文件
     */
    private void saveReport(String testName, String report, TestStatistics stats) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = String.format("load_test_%s_%s.txt", testName, timestamp);
            Path filePath = Paths.get(OUTPUT_DIR, fileName);

            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath.toFile()))) {
                writer.println(report);
            }

            // 同时保存CSV格式的详细数据
            String csvFileName = String.format("load_test_%s_%s.csv", testName, timestamp);
            Path csvPath = Paths.get(OUTPUT_DIR, csvFileName);
            stats.saveDetailedCsv(csvPath.toString());

            log.info("报告已保存: {}", filePath);
            log.info("详细数据已保存: {}", csvPath);
        } catch (Exception e) {
            log.error("保存报告失败", e);
        }
    }

    /**
     * 测试统计类
     */
    private static class TestStatistics {
        private final String testName;
        private final int totalFiles;
        private final int concurrentThreads;

        // 时间记录
        private long uploadStartTime;
        private long uploadEndTime;
        private long analysisStartTime;
        private long analysisEndTime;
        private long executionStartTime;
        private long executionEndTime;

        // 上传统计
        private final List<Long> uploadTimes = Collections.synchronizedList(new ArrayList<>());
        private final AtomicInteger uploadSuccessCount = new AtomicInteger(0);
        private final AtomicInteger uploadFailCount = new AtomicInteger(0);

        // 分析任务提交统计
        private final List<Long> analysisSubmitTimes = Collections.synchronizedList(new ArrayList<>());
        private final AtomicInteger analysisSubmitSuccessCount = new AtomicInteger(0);
        private final AtomicInteger analysisSubmitFailCount = new AtomicInteger(0);

        // 任务完成统计
        private int completedTasks = 0;
        private int failedTasks = 0;

        public TestStatistics(String testName, int totalFiles, int concurrentThreads) {
            this.testName = testName;
            this.totalFiles = totalFiles;
            this.concurrentThreads = concurrentThreads;
        }

        public void startUploadPhase() { uploadStartTime = System.currentTimeMillis(); }
        public void endUploadPhase() { uploadEndTime = System.currentTimeMillis(); }
        public void startAnalysisPhase() { analysisStartTime = System.currentTimeMillis(); }
        public void endAnalysisPhase() { analysisEndTime = System.currentTimeMillis(); }
        public void startExecutionPhase() { executionStartTime = System.currentTimeMillis(); }
        public void endExecutionPhase() { executionEndTime = System.currentTimeMillis(); }

        public void recordUpload(long timeMs, boolean success) {
            if (success) {
                uploadTimes.add(timeMs);
                uploadSuccessCount.incrementAndGet();
            } else {
                uploadFailCount.incrementAndGet();
            }
        }

        public void recordAnalysisSubmit(long timeMs, boolean success) {
            if (success) {
                analysisSubmitTimes.add(timeMs);
                analysisSubmitSuccessCount.incrementAndGet();
            } else {
                analysisSubmitFailCount.incrementAndGet();
            }
        }

        public void recordTaskComplete(boolean success) {
            // 由外部设置
        }

        public void setCompletedTasks(int count) { this.completedTasks = count; }
        public void setFailedTasks(int count) { this.failedTasks = count; }

        public String generateReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n");
            sb.append("================================================================================\n");
            sb.append("                         并发压力测试报告\n");
            sb.append("================================================================================\n");
            sb.append(String.format("测试名称: %s\n", testName));
            sb.append(String.format("测试时间: %s\n", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            sb.append(String.format("总文件数: %d\n", totalFiles));
            sb.append(String.format("并发线程数: %d\n", concurrentThreads));
            sb.append("\n");

            // 上传阶段统计
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("阶段1: 文件上传\n");
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append(String.format("  成功: %d, 失败: %d\n", uploadSuccessCount.get(), uploadFailCount.get()));
            sb.append(String.format("  总耗时: %.2f 秒\n", (uploadEndTime - uploadStartTime) / 1000.0));
            if (!uploadTimes.isEmpty()) {
                sb.append(String.format("  平均耗时: %.2f ms\n", uploadTimes.stream().mapToLong(l -> l).average().orElse(0)));
                sb.append(String.format("  最大耗时: %d ms\n", uploadTimes.stream().mapToLong(l -> l).max().orElse(0)));
                sb.append(String.format("  最小耗时: %d ms\n", uploadTimes.stream().mapToLong(l -> l).min().orElse(0)));
                sb.append(String.format("  吞吐量: %.2f 个/秒\n", uploadSuccessCount.get() * 1000.0 / (uploadEndTime - uploadStartTime)));
            }
            sb.append("\n");

            // 任务提交阶段统计
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("阶段2: 任务提交\n");
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append(String.format("  成功: %d, 失败: %d\n", analysisSubmitSuccessCount.get(), analysisSubmitFailCount.get()));
            sb.append(String.format("  总耗时: %.2f 秒\n", (analysisEndTime - analysisStartTime) / 1000.0));
            if (!analysisSubmitTimes.isEmpty()) {
                sb.append(String.format("  平均耗时: %.2f ms\n", analysisSubmitTimes.stream().mapToLong(l -> l).average().orElse(0)));
                sb.append(String.format("  最大耗时: %d ms\n", analysisSubmitTimes.stream().mapToLong(l -> l).max().orElse(0)));
                sb.append(String.format("  最小耗时: %d ms\n", analysisSubmitTimes.stream().mapToLong(l -> l).min().orElse(0)));
                sb.append(String.format("  吞吐量: %.2f 个/秒\n", analysisSubmitSuccessCount.get() * 1000.0 / (analysisEndTime - analysisStartTime)));
            }
            sb.append("\n");

            // 任务执行阶段统计
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("阶段3: 任务执行\n");
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append(String.format("  完成: %d, 失败: %d\n", completedTasks, failedTasks));
            sb.append(String.format("  总耗时: %.2f 秒\n", (executionEndTime - executionStartTime) / 1000.0));
            if (completedTasks > 0) {
                double avgExecutionTime = (executionEndTime - executionStartTime) / 1000.0 / completedTasks;
                sb.append(String.format("  平均执行时间: %.2f 秒/任务\n", avgExecutionTime));
            }
            sb.append("\n");

            // 总体统计
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("总体统计\n");
            sb.append("--------------------------------------------------------------------------------\n");
            long totalTime = executionEndTime - uploadStartTime;
            sb.append(String.format("  总测试时间: %.2f 秒 (%.2f 分钟)\n", totalTime / 1000.0, totalTime / 60000.0));
            sb.append(String.format("  成功率: %.2f%%\n", completedTasks * 100.0 / totalFiles));
            sb.append(String.format("  总吞吐量: %.2f 任务/分钟\n", completedTasks * 60000.0 / totalTime));
            sb.append("\n");
            sb.append("================================================================================\n");

            return sb.toString();
        }

        public void saveDetailedCsv(String filePath) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
                writer.println("metric,value");
                writer.println("test_name," + testName);
                writer.println("total_files," + totalFiles);
                writer.println("concurrent_threads," + concurrentThreads);
                writer.println("upload_success," + uploadSuccessCount.get());
                writer.println("upload_fail," + uploadFailCount.get());
                writer.println("upload_time_ms," + (uploadEndTime - uploadStartTime));
                writer.println("analysis_submit_success," + analysisSubmitSuccessCount.get());
                writer.println("analysis_submit_fail," + analysisSubmitFailCount.get());
                writer.println("analysis_submit_time_ms," + (analysisEndTime - analysisStartTime));
                writer.println("tasks_completed," + completedTasks);
                writer.println("tasks_failed," + failedTasks);
                writer.println("execution_time_ms," + (executionEndTime - executionStartTime));
                writer.println("total_time_ms," + (executionEndTime - uploadStartTime));
            } catch (Exception e) {
                log.error("保存CSV失败", e);
            }
        }
    }
}
