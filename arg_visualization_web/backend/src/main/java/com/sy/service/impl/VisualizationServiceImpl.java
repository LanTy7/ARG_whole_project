package com.sy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sy.mapper.AllPredictionMapper;
import com.sy.mapper.AnalysisTaskMapper;
import com.sy.mapper.ClassSummaryMapper;
import com.sy.pojo.AllPrediction;
import com.sy.pojo.AnalysisTask;
import com.sy.pojo.ClassSummary;
import com.sy.service.VisualizationService;
import com.sy.exception.TaskCancelledException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 可视化服务实现
 * 处理 ARG 抗性基因检测结果的可视化
 */
@Service
@RequiredArgsConstructor
public class VisualizationServiceImpl implements VisualizationService {

    private static final Logger log = LoggerFactory.getLogger(VisualizationServiceImpl.class);
    /**
     * 落库批量大小：一条 SQL 插入多行。
     * 注意：单条 INSERT 的体积不能超过 MySQL 的 max_allowed_packet（当前约 64MB），
     * 每条记录包含完整 sequence_id 等字段，实际大小可能较大，因此这里采用保守值 5000，
     * 对于百万级结果集，大约需要 200 次 INSERT，既安全又足够快。
     */
    private static final int BATCH_SIZE = 5_000;

    private final AnalysisTaskMapper analysisTaskMapper;
    private final AllPredictionMapper allPredictionMapper;
    private final ClassSummaryMapper classSummaryMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${analysis.output-dir:./genome_outputs}")
    private String outputDir;

    /** 等待输出文件就绪的最大毫秒数（Docker 可能尚未刷盘，大文件需要更长时间） */
    private static final int WAIT_FOR_FILE_MS = 300_000;  // 5分钟
    private static final int WAIT_POLL_MS = 500;  // 轮询间隔500ms

    @Override
    public void persistTaskResultsToDb(Long taskId) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null || task.getOutputDir() == null) {
            log.warn("任务不存在或无输出目录: taskId={}", taskId);
            return;
        }
        Path basePath = Paths.get(task.getOutputDir());
        Path allPath = waitForFile(basePath, "all_predictions.tsv");
        if (allPath == null) {
            log.warn("未找到 all_predictions.tsv: taskId={}, basePath={}", taskId, basePath);
            return;
        }
        // 同样等待 class_summary.tsv 文件出现（Docker 可能还在写）
        Path classPath = waitForFile(basePath, "class_summary.tsv");
        try {
            allPredictionMapper.delete(new LambdaQueryWrapper<AllPrediction>().eq(AllPrediction::getTaskId, taskId));
            classSummaryMapper.delete(new LambdaQueryWrapper<ClassSummary>().eq(ClassSummary::getTaskId, taskId));

            // 落库前若已被取消则直接退出，不写数据
            if (isTaskCancelled(taskId)) {
                log.info("任务已取消，跳过落库: taskId={}", taskId);
                throw new TaskCancelledException(taskId);
            }

            int totalCount = 0;
            int argCount = 0;
            List<AllPrediction> batch = new ArrayList<>(BATCH_SIZE);
            try (BufferedReader reader = Files.newBufferedReader(allPath)) {
                String line;
                String[] headers = null;
                Map<String, Integer> headerIndex = new HashMap<>();
                int rowIndex = 1;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty() || line.startsWith("#")) continue;
                    String[] values = line.split("\t", -1);
                    if (headers == null) {
                        headers = values;
                        for (int i = 0; i < headers.length; i++)
                            headerIndex.put(headers[i].toLowerCase().trim(), i);
                        continue;
                    }
                    // 每批插入前检查是否已取消，避免取消后仍落库并最终覆盖为 COMPLETED
                    if (batch.size() >= BATCH_SIZE) {
                        if (isTaskCancelled(taskId)) {
                            allPredictionMapper.delete(new LambdaQueryWrapper<AllPrediction>().eq(AllPrediction::getTaskId, taskId));
                            classSummaryMapper.delete(new LambdaQueryWrapper<ClassSummary>().eq(ClassSummary::getTaskId, taskId));
                            log.info("任务已取消，停止落库并清理已写入数据: taskId={}", taskId);
                            throw new TaskCancelledException(taskId);
                        }
                        allPredictionMapper.insertBatch(batch);
                        batch.clear();
                    }
                    String idVal = getVal(values, headerIndex, "id", "");
                    boolean isArg = "true".equalsIgnoreCase(getVal(values, headerIndex, "is_arg", "false"));
                    Double binaryProb = parseDouble(getVal(values, headerIndex, "binary_prob", null));
                    String argClass = getVal(values, headerIndex, "arg_class", null);
                    if (argClass != null) argClass = argClass.trim();
                    if (argClass == null) argClass = "";
                    Double classProb = parseDouble(getVal(values, headerIndex, "class_prob", null));

                    AllPrediction p = new AllPrediction();
                    p.setTaskId(taskId);
                    p.setRowIndex(rowIndex++);
                    p.setSequenceId(idVal != null ? idVal : "");
                    p.setIsArg(isArg);
                    p.setBinaryProb(binaryProb);
                    p.setArgClass(argClass);
                    p.setClassProb(classProb);
                    batch.add(p);
                    totalCount++;
                    if (isArg) argCount++;
                }
                if (!batch.isEmpty()) {
                    if (isTaskCancelled(taskId)) {
                        allPredictionMapper.delete(new LambdaQueryWrapper<AllPrediction>().eq(AllPrediction::getTaskId, taskId));
                        classSummaryMapper.delete(new LambdaQueryWrapper<ClassSummary>().eq(ClassSummary::getTaskId, taskId));
                        log.info("任务已取消，停止落库并清理已写入数据: taskId={}", taskId);
                        throw new TaskCancelledException(taskId);
                    }
                    allPredictionMapper.insertBatch(batch);
                }
            }

            if (classPath != null && Files.exists(classPath)) {
                List<ClassSummary> classSummaryList = new ArrayList<>();
                try (BufferedReader reader = Files.newBufferedReader(classPath)) {
                    String line;
                    String[] headers = null;
                    Map<String, Integer> headerIndex = new HashMap<>();
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty() || line.startsWith("#")) continue;
                        String[] values = line.split("\t", -1);
                        if (headers == null) {
                            headers = values;
                            for (int i = 0; i < headers.length; i++)
                                headerIndex.put(headers[i].toLowerCase().trim(), i);
                            continue;
                        }
                        String argClass = getVal(values, headerIndex, "arg_class", null);
                        if (argClass != null) argClass = argClass.trim();
                        if (argClass == null) argClass = "";
                        int count = 0;
                        try {
                            String countStr = getVal(values, headerIndex, "count", "0");
                            if (countStr != null && !countStr.trim().isEmpty()) {
                                count = Integer.parseInt(countStr.trim());
                            }
                        } catch (NumberFormatException ignored) {}
                        ClassSummary cs = new ClassSummary();
                        cs.setTaskId(taskId);
                        cs.setArgClass(argClass);
                        cs.setCount(count);
                        classSummaryList.add(cs);
                    }
                }
                if (!classSummaryList.isEmpty()) {
                    classSummaryMapper.insertBatch(classSummaryList);
                }
                log.debug("任务 {} class_summary 已写入", taskId);
            } else {
                log.warn("未找到 class_summary.tsv: taskId={}", taskId);
            }

            // 重新加载任务再更新，只改 total_count / prophage_count，避免覆盖其他字段
            AnalysisTask toUpdate = analysisTaskMapper.selectById(taskId);
            if (toUpdate != null) {
                toUpdate.setTotalCount(totalCount);
                toUpdate.setProphageCount(argCount);
                analysisTaskMapper.updateById(toUpdate);
            }
            log.info("任务 {} 结果已落库: total={}, arg={}, class_summary={}", taskId, totalCount, argCount, classPath != null && Files.exists(classPath));
        } catch (Exception e) {
            log.error("落库失败: taskId={}", taskId, e);
            throw new RuntimeException("落库失败: " + e.getMessage(), e);
        }
    }

    /** 
     * 等待文件出现并写完（Docker 可能尚未刷盘），最多等待 WAIT_FOR_FILE_MS 毫秒
     * 文件出现后，继续等待直到文件大小稳定（不再增长）
     */
    private Path waitForFile(Path basePath, String fileName) {
        long deadline = System.currentTimeMillis() + WAIT_FOR_FILE_MS;
        Path foundPath = null;
        
        // 阶段1：等待文件出现
        while (System.currentTimeMillis() < deadline) {
            Path p = resolveFile(basePath, fileName);
            if (p != null) {
                foundPath = p;
                break;
            }
            try {
                Thread.sleep(WAIT_POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (foundPath == null) {
            return resolveFile(basePath, fileName);
        }
        
        // 阶段2：等待文件写完（文件大小稳定）
        long lastSize = -1;
        int stableCount = 0;
        final int STABLE_THRESHOLD = 3; // 连续3次大小相同才认为写完
        
        while (System.currentTimeMillis() < deadline && stableCount < STABLE_THRESHOLD) {
            try {
                long currentSize = Files.size(foundPath);
                if (currentSize == lastSize && currentSize > 0) {
                    stableCount++;
                } else {
                    stableCount = 0;
                    lastSize = currentSize;
                }
                if (stableCount < STABLE_THRESHOLD) {
                    Thread.sleep(WAIT_POLL_MS);
                }
            } catch (IOException e) {
                log.warn("检查文件大小失败: {}", foundPath, e);
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (stableCount >= STABLE_THRESHOLD) {
            log.debug("文件 {} 已稳定，大小: {} bytes", fileName, lastSize);
        } else {
            log.warn("文件 {} 可能未完全写完，当前大小: {} bytes", fileName, lastSize);
        }
        
        return foundPath;
    }

    private Path resolveFile(Path basePath, String fileName) {
        Path p = basePath.resolve(fileName);
        if (Files.exists(p)) return p;
        p = basePath.resolve("arg").resolve(fileName);
        return Files.exists(p) ? p : null;
    }

    private static String getVal(String[] values, Map<String, Integer> headerIndex, String key, String def) {
        Integer i = headerIndex.get(key.toLowerCase());
        if (i != null && i < values.length && values[i] != null) return values[i];
        return def;
    }

    /** 判断任务是否已被用户取消（落库过程中可被取消） */
    private boolean isTaskCancelled(Long taskId) {
        AnalysisTask t = analysisTaskMapper.selectById(taskId);
        return t != null && "CANCELLED".equals(t.getStatus());
    }

    @Override
    public Map<String, Object> getSummary(Long taskId, Long userId) {
        AnalysisTask task = validateTask(taskId, userId);
        int total = task.getTotalCount() != null ? task.getTotalCount() : 0;
        int arg = task.getProphageCount() != null ? task.getProphageCount() : 0;
        // 若 analysis_tasks 未落库（total_count 为空）或 抗性基因数为0但应该有数据，从 all_predictions 表统计并回填
        boolean needRecalculate = total <= 0 || (arg <= 0 && total > 0);
        if (needRecalculate) {
            Long totalFromDb = allPredictionMapper.selectCount(new LambdaQueryWrapper<AllPrediction>().eq(AllPrediction::getTaskId, taskId));
            if (totalFromDb != null && totalFromDb > 0) {
                total = totalFromDb.intValue();
                Long argFromDb = allPredictionMapper.selectCount(new LambdaQueryWrapper<AllPrediction>()
                        .eq(AllPrediction::getTaskId, taskId).eq(AllPrediction::getIsArg, true));
                arg = argFromDb != null ? argFromDb.intValue() : 0;
                log.info("从 all_predictions 表重新计算统计: taskId={}, total={}, arg={}", taskId, total, arg);
                // 回填 analysis_tasks 表
                AnalysisTask toUpdate = analysisTaskMapper.selectById(taskId);
                if (toUpdate != null && (toUpdate.getTotalCount() == null || !toUpdate.getTotalCount().equals(total)
                        || toUpdate.getProphageCount() == null || !toUpdate.getProphageCount().equals(arg))) {
                    toUpdate.setTotalCount(total);
                    toUpdate.setProphageCount(arg);
                    analysisTaskMapper.updateById(toUpdate);
                    log.info("已回填 analysis_tasks: taskId={}, total={}, arg={}", taskId, total, arg);
                }
            }
        }
        Map<String, Object> genomeInfo = new HashMap<>();
        genomeInfo.put("taskId", taskId);
        genomeInfo.put("taskName", task.getTaskName());
        genomeInfo.put("status", task.getStatus());
        genomeInfo.put("totalCount", total);
        genomeInfo.put("argCount", arg);
        genomeInfo.put("nonArgCount", Math.max(0, total - arg));
        return Collections.singletonMap("genomeInfo", genomeInfo);
    }

    @Override
    public Map<String, Object> getResultsPage(Long taskId, Long userId, int page, int pageSize, Boolean isArg, String keyword) {
        validateTask(taskId, userId);
        LambdaQueryWrapper<AllPrediction> q = new LambdaQueryWrapper<AllPrediction>()
                .eq(AllPrediction::getTaskId, taskId)
                .orderByAsc(AllPrediction::getRowIndex);
        if (isArg != null) {
            q.eq(AllPrediction::getIsArg, isArg);
        }
        if (StringUtils.hasText(keyword)) {
            String k = "%" + keyword.trim() + "%";
            q.and(w -> w.like(AllPrediction::getSequenceId, k).or().like(AllPrediction::getArgClass, k));
        }
        long total = allPredictionMapper.selectCount(q);
        int offset = (page - 1) * pageSize;
        List<AllPrediction> list = allPredictionMapper.selectList(q.last("LIMIT " + pageSize + " OFFSET " + offset));
        List<Map<String, Object>> argResults = list.stream().map(this::allPredictionToMap).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("argResults", argResults);
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("total", total);
        pagination.put("page", page);
        pagination.put("pageSize", pageSize);
        result.put("pagination", pagination);
        return result;
    }

    private Map<String, Object> allPredictionToMap(AllPrediction p) {
        Map<String, Object> m = new HashMap<>();
        m.put("index", p.getRowIndex());
        m.put("id", p.getSequenceId());
        m.put("isArg", Boolean.TRUE.equals(p.getIsArg()));
        m.put("predProb", p.getBinaryProb());
        m.put("argClass", p.getArgClass());
        m.put("classProb", p.getClassProb());
        m.put("topClasses", null);
        return m;
    }

    @Override
    public Map<String, Object> getClassSummary(Long taskId, Long userId) {
        validateTask(taskId, userId);
        List<ClassSummary> list = classSummaryMapper.selectList(
                new LambdaQueryWrapper<ClassSummary>().eq(ClassSummary::getTaskId, taskId).orderByDesc(ClassSummary::getCount));
        List<Map<String, Object>> items = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            for (ClassSummary cs : list) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", cs.getArgClass());
                item.put("value", cs.getCount());
                items.add(item);
            }
        } else {
            // class_summary 表无数据时，从 all_predictions 表聚合类别分布
            log.info("getClassSummary: class_summary 表无数据，从 all_predictions 聚合: taskId={}", taskId);
            List<AllPrediction> argPredictions = allPredictionMapper.selectList(
                    new LambdaQueryWrapper<AllPrediction>()
                            .eq(AllPrediction::getTaskId, taskId)
                            .eq(AllPrediction::getIsArg, true));
            Map<String, Long> classCount = new HashMap<>();
            for (AllPrediction p : argPredictions) {
                String argClass = p.getArgClass();
                if (argClass != null && !argClass.isEmpty()) {
                    classCount.merge(argClass, 1L, Long::sum);
                }
            }
            // 按数量降序排序
            classCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(e -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("name", e.getKey());
                        item.put("value", e.getValue());
                        items.add(item);
                    });
            // 回填到 class_summary 表
            if (!classCount.isEmpty()) {
                log.info("回填 class_summary 表: taskId={}, classCount={}", taskId, classCount.size());
                for (Map.Entry<String, Long> e : classCount.entrySet()) {
                    ClassSummary cs = new ClassSummary();
                    cs.setTaskId(taskId);
                    cs.setArgClass(e.getKey());
                    cs.setCount(e.getValue().intValue());
                    classSummaryMapper.insert(cs);
                }
            }
        }
        return Collections.singletonMap("classSummary", items);
    }

    @Override
    public Map<String, Object> getGenomeVisualization(Long taskId, Long userId) {
        AnalysisTask task = validateTask(taskId, userId);
        boolean useDb = task.getTotalCount() != null && task.getTotalCount() > 0;
        if (!useDb) {
            Long fromDb = allPredictionMapper.selectCount(new LambdaQueryWrapper<AllPrediction>().eq(AllPrediction::getTaskId, taskId));
            useDb = fromDb != null && fromDb > 0;
        }
        if (useDb) {
            Map<String, Object> summary = getSummary(taskId, userId);
            Map<String, Object> firstPage = getResultsPage(taskId, userId, 1, 100, null, null);
            Map<String, Object> out = new HashMap<>(summary);
            out.put("argResults", firstPage.get("argResults"));
            out.put("pagination", firstPage.get("pagination"));
            return out;
        }
        return getGenomeVisualizationFromFile(taskId, userId);
    }

    private Map<String, Object> getGenomeVisualizationFromFile(Long taskId, Long userId) {
        AnalysisTask task = validateTask(taskId, userId);
        String taskOutputDir = task.getOutputDir() != null ? task.getOutputDir() : Paths.get(outputDir, "task_" + taskId).toString();
        try {
            Map<String, Object> visualization = new HashMap<>();
            Map<String, Object> genomeInfo = new HashMap<>();
            genomeInfo.put("taskId", taskId);
            genomeInfo.put("taskName", task.getTaskName());
            genomeInfo.put("status", task.getStatus());
            genomeInfo.put("argCount", task.getProphageCount());
            genomeInfo.put("totalCount", task.getTotalCount());
            genomeInfo.put("nonArgCount", task.getTotalCount() != null && task.getProphageCount() != null
                    ? Math.max(0, task.getTotalCount() - task.getProphageCount()) : null);
            visualization.put("genomeInfo", genomeInfo);
            List<Map<String, Object>> argResults = parseArgResultsList(taskOutputDir);
            visualization.put("argResults", argResults);
            log.info("从文件加载任务 {} 可视化数据，共 {} 条", taskId, argResults.size());
            return visualization;
        } catch (Exception e) {
            log.error("读取可视化数据失败: taskId={}", taskId, e);
            throw new RuntimeException("读取可视化数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析 ARG 预测结果（公共方法，供 DockerServiceImpl 调用）
     */
    public Map<String, Object> parseArgOutput(String taskOutputDir) throws IOException {
        Map<String, Object> result = new HashMap<>();
        
        Path argFile = Paths.get(taskOutputDir, "arg_predictions.tsv");
        
        // 如果默认文件不存在，尝试查找其他 TSV 文件
        if (!Files.exists(argFile)) {
            log.warn("未找到 arg_predictions.tsv，尝试查找其他文件");
            
            // 先检查 arg 子目录（MAG 任务）
            Path argSubDir = Paths.get(taskOutputDir, "arg");
            File outputDirFile;
            if (Files.exists(argSubDir) && Files.isDirectory(argSubDir)) {
                outputDirFile = argSubDir.toFile();
            } else {
                outputDirFile = new File(taskOutputDir);
            }
            
            File[] files = outputDirFile.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().endsWith(".tsv")) {
                        argFile = f.toPath();
                        log.info("找到备用文件: {}", f.getName());
                        break;
                    }
                }
            }
        }
        
        List<Map<String, Object>> argResults;
        if (Files.exists(argFile)) {
            argResults = parseArgResultsListFromFile(argFile);
        } else {
            log.warn("未找到任何 TSV 输出文件，返回空结果");
            argResults = new ArrayList<>();
        }
        
        result.put("argCount", argResults.size());
        result.put("argResults", argResults);
        return result;
    }
    
    /**
     * 解析 ARG 预测结果（返回列表）
     */
    private List<Map<String, Object>> parseArgResultsList(String taskOutputDir) throws IOException {
        // 优先查找 all_predictions.tsv
        Path argFile = Paths.get(taskOutputDir, "all_predictions.tsv");
        
        if (!Files.exists(argFile)) {
            // 尝试 arg_predictions.tsv
            argFile = Paths.get(taskOutputDir, "arg_predictions.tsv");
        }
        
        // 如果根目录没找到，检查 arg 子目录（MAG 任务）
        if (!Files.exists(argFile)) {
            Path argSubDir = Paths.get(taskOutputDir, "arg");
            if (Files.exists(argSubDir) && Files.isDirectory(argSubDir)) {
                argFile = argSubDir.resolve("all_predictions.tsv");
                if (!Files.exists(argFile)) {
                    argFile = argSubDir.resolve("arg_predictions.tsv");
                }
                log.info("在 arg 子目录查找输出文件: {}", argFile);
            }
        }
        
        if (!Files.exists(argFile)) {
            log.warn("ARG 输出文件不存在: {}", taskOutputDir);
            return new ArrayList<>();
        }
        
        return parseArgResultsListFromFile(argFile);
    }
    
    /**
     * 从指定文件解析 ARG 预测结果
     * TSV 格式: id, is_arg, binary_prob, arg_class, class_prob, top_classes
     */
    private List<Map<String, Object>> parseArgResultsListFromFile(Path argFile) throws IOException {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(argFile)) {
            String line;
            String[] headers = null;
            Map<String, Integer> headerIndex = new HashMap<>();
            int index = 1;
            
            while ((line = reader.readLine()) != null) {
                // 跳过空行和注释
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                String[] values = line.split("\t", -1); // -1 保留尾部空字符串
                
                // 第一行是表头，建立索引映射
                if (headers == null) {
                    headers = values;
                    for (int i = 0; i < headers.length; i++) {
                        headerIndex.put(headers[i].toLowerCase().trim(), i);
                    }
                    log.debug("TSV 表头: {}", Arrays.toString(headers));
                    continue;
                }
                
                // 解析数据行
                Map<String, Object> result = new HashMap<>();
                result.put("index", index++);
                     
                // 根据表头动态解析
                result.put("id", getValueByHeader(values, headerIndex, "id", ""));
                result.put("isArg", "True".equalsIgnoreCase(getValueByHeader(values, headerIndex, "is_arg", "false")));
                result.put("predProb", parseDouble(getValueByHeader(values, headerIndex, "binary_prob", null)));
                result.put("argClass", getValueByHeader(values, headerIndex, "arg_class", ""));
                result.put("classProb", parseDouble(getValueByHeader(values, headerIndex, "class_prob", null)));
                
                // 解析 top_classes JSON 字段（新增）
                String topClassesJson = getValueByHeader(values, headerIndex, "top_classes", null);
                if (topClassesJson != null && !topClassesJson.isEmpty() && !"null".equalsIgnoreCase(topClassesJson)) {
                    try {
                        // 处理 pandas 导出的 CSV/TSV 格式：外层有引号，内部引号被转义为 ""
                        String cleanJson = topClassesJson;
                        // 去掉外层引号（如果有）
                        if (cleanJson.startsWith("\"") && cleanJson.endsWith("\"")) {
                            cleanJson = cleanJson.substring(1, cleanJson.length() - 1);
                        }
                        // 将转义的双引号 "" 替换为 "
                        cleanJson = cleanJson.replace("\"\"", "\"");
                        // 去掉首尾的转义残留（如果存在）
                        cleanJson = cleanJson.trim();
                            
                        // 解析 JSON 数组
                        List<Map<String, Object>> topClasses = objectMapper.readValue(
                            cleanJson, 
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                        );
                        result.put("topClasses", topClasses);
                    } catch (Exception e) {
                        log.warn("解析 top_classes JSON 失败: {}", topClassesJson, e);
                        result.put("topClasses", null);
                    }
                } else {
                    result.put("topClasses", null);
                }
                
                results.add(result);
            }
        }
        
        log.info("解析到 {} 个 ARG 预测结果", results.size());
        return results;
    }
    
    /**
     * 根据表头名称获取值
     */
    private String getValueByHeader(String[] values, Map<String, Integer> headerIndex, String headerName, String defaultValue) {
        Integer idx = headerIndex.get(headerName.toLowerCase());
        if (idx != null && idx < values.length) {
            String value = values[idx];
            if (value != null && !value.trim().isEmpty()) {
                return value.trim(); 
            }
        }
        return defaultValue;
    }
    
    /**
     * 安全解析 double
     */
    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            log.warn("无法解析数值: {}", value);
            return null;
        }
    }

    @Override
    public Map<String, Object> getProphageDetail(Long taskId, Long regionId, Long userId) {
        throw new UnsupportedOperationException("此功能不再支持");
    }

    @Override
    public Map<String, Object> getStatistics(Long taskId, Long userId) {
        AnalysisTask task = validateTask(taskId, userId);
        Map<String, Object> statistics = new HashMap<>();
        int total = task.getTotalCount() != null ? task.getTotalCount() : 0;
        int argCount = task.getProphageCount() != null ? task.getProphageCount() : 0;
        // 若 analysis_tasks 未落库（total_count 为空）或 抗性基因数为0但应该有数据，从 all_predictions 表统计
        boolean needRecalculate = total <= 0 || (argCount <= 0 && total > 0);
        if (needRecalculate) {
            Long totalFromDb = allPredictionMapper.selectCount(new LambdaQueryWrapper<AllPrediction>().eq(AllPrediction::getTaskId, taskId));
            if (totalFromDb != null && totalFromDb > 0) {
                total = totalFromDb.intValue();
                Long argFromDb = allPredictionMapper.selectCount(new LambdaQueryWrapper<AllPrediction>()
                        .eq(AllPrediction::getTaskId, taskId).eq(AllPrediction::getIsArg, true));
                argCount = argFromDb != null ? argFromDb.intValue() : 0;
                log.info("getStatistics 从 all_predictions 表重新计算: taskId={}, total={}, arg={}", taskId, total, argCount);
                // 回填 analysis_tasks 表
                AnalysisTask toUpdate = analysisTaskMapper.selectById(taskId);
                if (toUpdate != null && (toUpdate.getTotalCount() == null || !toUpdate.getTotalCount().equals(total)
                        || toUpdate.getProphageCount() == null || !toUpdate.getProphageCount().equals(argCount))) {
                    toUpdate.setTotalCount(total);
                    toUpdate.setProphageCount(argCount);
                    analysisTaskMapper.updateById(toUpdate);
                }
            }
        }
        if (total > 0) {
            statistics.put("totalSequences", total);
            statistics.put("argCount", argCount);
            statistics.put("nonArgCount", Math.max(0, total - argCount));
            // 优先从 class_summary 表查询
            List<ClassSummary> list = classSummaryMapper.selectList(
                    new LambdaQueryWrapper<ClassSummary>().eq(ClassSummary::getTaskId, taskId));
            Map<String, Long> classDistribution = new HashMap<>();
            if (list != null && !list.isEmpty()) {
                for (ClassSummary cs : list) {
                    classDistribution.put(cs.getArgClass(), cs.getCount().longValue());
                }
            } else {
                // class_summary 表无数据时，从 all_predictions 表聚合类别分布
                log.info("class_summary 表无数据，从 all_predictions 聚合: taskId={}", taskId);
                List<AllPrediction> argPredictions = allPredictionMapper.selectList(
                        new LambdaQueryWrapper<AllPrediction>()
                                .eq(AllPrediction::getTaskId, taskId)
                                .eq(AllPrediction::getIsArg, true));
                for (AllPrediction p : argPredictions) {
                    String argClass = p.getArgClass();
                    if (argClass != null && !argClass.isEmpty()) {
                        classDistribution.merge(argClass, 1L, Long::sum);
                    }
                }
            }
            statistics.put("classDistribution", classDistribution);
            return statistics;
        }
        try {
            String taskOutputDir = task.getOutputDir() != null ? task.getOutputDir() : Paths.get(outputDir, "task_" + taskId).toString();
            List<Map<String, Object>> argResults = parseArgResultsList(taskOutputDir);
            long argCountFromFile = argResults.stream().filter(r -> Boolean.TRUE.equals(r.get("isArg"))).count();
            statistics.put("totalSequences", argResults.size());
            statistics.put("argCount", argCountFromFile);
            statistics.put("nonArgCount", argResults.size() - (int) argCountFromFile);
            Map<String, Long> classDistribution = new HashMap<>();
            for (Map<String, Object> result : argResults) {
                if (Boolean.TRUE.equals(result.get("isArg"))) {
                    String argClass = (String) result.get("argClass");
                    if (argClass != null && !argClass.isEmpty()) {
                        classDistribution.merge(argClass, 1L, Long::sum);
                    }
                }
            }
            statistics.put("classDistribution", classDistribution);
            return statistics;
        } catch (Exception e) {
            log.error("生成统计数据失败: taskId={}", taskId, e);
            throw new RuntimeException("生成统计数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> exportVisualizationData(Long taskId, Long userId) {
        // 验证任务
        AnalysisTask task = validateTask(taskId, userId);
        
        try {
            Map<String, Object> exportData = new HashMap<>();
            
            // 任务信息
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("taskId", task.getTaskId());
            taskInfo.put("taskName", task.getTaskName());
            taskInfo.put("status", task.getStatus());
            taskInfo.put("createdAt", task.getCreatedAt() != null ? task.getCreatedAt().toString() : null);
            taskInfo.put("completedAt", task.getCompletedAt() != null ? task.getCompletedAt().toString() : null);
            exportData.put("taskInfo", taskInfo);
            
            // 可视化数据
            exportData.put("visualization", getGenomeVisualization(taskId, userId));
            
            // 统计数据
            exportData.put("statistics", getStatistics(taskId, userId));
            
            log.info("成功导出任务 {} 的完整数据", taskId);
            return exportData;
            
        } catch (Exception e) {
            log.error("导出数据失败: taskId={}", taskId, e);
            throw new RuntimeException("导出数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证任务
     */
    private AnalysisTask validateTask(Long taskId, Long userId) {
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
        return task;
    }
}
