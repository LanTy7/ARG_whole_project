package com.sy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sy.mapper.AnalysisTaskMapper;
import com.sy.mapper.GenomeFileMapper;
import com.sy.mapper.LoginLogMapper;
import com.sy.mapper.UserMapper;
import com.sy.pojo.AnalysisTask;
import com.sy.pojo.GenomeFile;
import com.sy.pojo.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sy.service.AdminService;
import com.sy.service.AnalysisTaskService;
import com.sy.service.GenomeFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final GenomeFileMapper genomeFileMapper;
    private final AnalysisTaskMapper analysisTaskMapper;
    private final LoginLogMapper loginLogMapper;
    private final ObjectMapper objectMapper;
    private final GenomeFileService genomeFileService;
    private final AnalysisTaskService analysisTaskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        log.info("开始删除用户: userId={}", userId);

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 1. 该用户下所有任务（含文件关联 + MAG 等），先批量删表再按任务删目录和记录
        List<AnalysisTask> allTasks = analysisTaskMapper.findByUserId(userId);
        if (!allTasks.isEmpty()) {
            try {
                analysisTaskService.deleteTasksAndRelatedDataBatch(allTasks);
            } catch (Exception e) {
                log.error("批量删除用户关联任务失败: userId={}", userId, e);
            }
        }

        // 2. 删除用户下所有文件（物理文件 + 数据库记录）
        List<GenomeFile> files = genomeFileMapper.findByUserId(userId);
        for (GenomeFile file : files) {
            try {
                if (file.getFilePath() != null) {
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(file.getFilePath()));
                }
                genomeFileMapper.deleteById(file.getFileId());
            } catch (Exception e) {
                log.error("删除用户文件失败: fileId={}", file.getFileId(), e);
            }
        }

        // 3. 删除用户记录
        userMapper.deleteById(userId);
        log.info("用户删除成功: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long fileId) {
        log.info("管理员删除文件: fileId={}", fileId);

        GenomeFile file = genomeFileMapper.selectById(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }

        genomeFileService.deleteFileAndRelatedData(file);
        log.info("文件删除成功: fileId={}", fileId);
    }

    @Override
    public List<Map<String, Object>> getAllUsers() {
        List<User> users = userMapper.selectList(null);
        return users.stream()
                .map(this::convertUserToMap)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getAllFiles() {
        List<GenomeFile> files = genomeFileMapper.selectList(null);
        return files.stream()
                .map(this::convertFileToMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getUsersPage(int pageNum, int pageSize, String keyword) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            wrapper.and(w -> w.like("username", k).or().like("email", k));
        }
        wrapper.orderByDesc("created_at");
        IPage<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Map<String, Object>> list = page.getRecords().stream()
                .map(this::convertUserToMap)
                .collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    @Override
    public Map<String, Object> getFilesPage(int pageNum, int pageSize, String userKeyword, String fileKeyword) {
        String userK = StringUtils.hasText(userKeyword) ? userKeyword.trim() : null;
        String fileK = StringUtils.hasText(fileKeyword) ? fileKeyword.trim() : null;
        if (userK == null && fileK == null) {
            QueryWrapper<GenomeFile> wrapper = new QueryWrapper<>();
            wrapper.orderByDesc("upload_time");
            IPage<GenomeFile> page = genomeFileMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
            List<Map<String, Object>> list = page.getRecords().stream()
                    .map(this::convertFileToMap)
                    .collect(Collectors.toList());
            Map<String, Object> result = new HashMap<>();
            result.put("list", list);
            result.put("total", page.getTotal());
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            return result;
        }
        IPage<GenomeFile> page = genomeFileMapper.searchFilesWithConditionsPage(
                new Page<>(pageNum, pageSize), userK, fileK);
        List<Map<String, Object>> list = page.getRecords().stream()
                .map(this::convertFileToMap)
                .collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    @Override
    @Transactional
    public void banUser(Long userId, Boolean ban) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        user.setStatus(ban ? "BANNED" : "ACTIVE");
        userMapper.updateById(user);
        
        log.info("用户状态更新: userId={}, status={}", userId, user.getStatus());
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 用户统计
        Long totalUsers = userMapper.selectCount(null);
        stats.put("totalUsers", totalUsers);
        
        // 文件统计
        Long totalFiles = genomeFileMapper.selectCount(null);
        stats.put("totalFiles", totalFiles);
        
        // 任务统计
        Long totalTasks = analysisTaskMapper.selectCount(null);
        stats.put("totalTasks", totalTasks);
        
        // 登录统计
        Long totalLogins = loginLogMapper.countByStatus("SUCCESS");
        stats.put("totalLogins", totalLogins);
        
        return stats;
    }

    @Override
    public List<Map<String, Object>> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllUsers();
        }
        
        List<User> users;
        // 尝试将关键字解析为数字（用户ID）
        try {
            Long userId = Long.parseLong(keyword.trim());
            // 如果是数字，按ID查找
            User user = userMapper.findById(userId);
            users = user != null ? List.of(user) : new ArrayList<>();
        } catch (NumberFormatException e) {
            // 如果不是数字，按用户名查找
            users = userMapper.selectUsersWithKeyword(keyword.trim());
        }
        
        return users.stream()
                .map(this::convertUserToMap)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> searchFiles(String userKeyword, String fileKeyword) {
        // 如果两个关键字都为空，返回所有文件
        if ((userKeyword == null || userKeyword.trim().isEmpty()) && 
            (fileKeyword == null || fileKeyword.trim().isEmpty())) {
            return getAllFiles();
        }
        
        String userKeywordTrimmed = userKeyword != null ? userKeyword.trim() : "";
        String fileKeywordTrimmed = fileKeyword != null ? fileKeyword.trim() : "";
        
        List<GenomeFile> files = genomeFileMapper.searchFilesWithConditions(
            userKeywordTrimmed.isEmpty() ? null : userKeywordTrimmed,
            fileKeywordTrimmed.isEmpty() ? null : fileKeywordTrimmed
        );
        
        return files.stream()
                .map(this::convertFileToMap)
                .collect(Collectors.toList());
    }

    /**
     * 转换用户为Map
     */
    private Map<String, Object> convertUserToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getUserId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("nickname", user.getNickname());
        map.put("role", user.getRole());
        map.put("status", user.getStatus());
        map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        map.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null);
        
        // 获取最后登录的地理位置（DB 存 JSON：{"zh":"浙江省嘉兴市","en":"Jiaxing, Zhejiang"}，兼容旧数据为纯中文串）
        com.sy.pojo.LoginLog lastLogin = loginLogMapper.findLastLoginByUserId(user.getUserId());
        if (lastLogin == null || lastLogin.getLocation() == null) {
            map.put("lastLoginLocation", null);
        } else {
            String loc = lastLogin.getLocation();
            try {
                Map<String, String> parsed = objectMapper.readValue(loc, new TypeReference<Map<String, String>>() {});
                map.put("lastLoginLocation", parsed);
            } catch (Exception e) {
                map.put("lastLoginLocation", Map.of("zh", loc, "en", loc));
            }
        }
        
        // 统计用户数据
        Integer fileCount = genomeFileMapper.countByUserId(user.getUserId());
        Integer taskCount = analysisTaskMapper.countByUserId(user.getUserId());
        map.put("fileCount", fileCount);
        map.put("taskCount", taskCount);
        
        return map;
    }

    /**
     * 转换文件为Map
     */
    private Map<String, Object> convertFileToMap(GenomeFile file) {
        Map<String, Object> map = new HashMap<>();
        map.put("fileId", file.getFileId());
        map.put("userId", file.getUserId());
        
        // 获取用户信息
        User user = userMapper.findById(file.getUserId());
        map.put("username", user != null ? user.getUsername() : "未知");
        
        map.put("originalFilename", file.getOriginalFilename());
        map.put("fileSize", file.getFileSize());
        map.put("fileType", file.getFileType());
        map.put("status", file.getStatus());
        map.put("uploadTime", file.getUploadTime() != null ? file.getUploadTime().toString() : null);
        
        return map;
    }
}

