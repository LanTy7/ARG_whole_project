package com.sy.controller;

import com.sy.pojo.User;
import com.sy.service.AdminService;
import com.sy.mapper.UserMapper;
import com.sy.util.JwtUtil;
import com.sy.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员控制器
 * 用于管理用户、文件、任务等
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    /**
     * 验证管理员权限
     */
    private void checkAdmin(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId != null) {
                User user = userMapper.findById(userId);
                if (user != null && "ADMIN".equals(user.getRole())) {
                    return;
                }
            }
        }
        throw new RuntimeException("无管理员权限");
    }

    /**
     * 获取所有用户列表（不分页，保留兼容）
     */
    @GetMapping("/users")
    public Result<List<Map<String, Object>>> getAllUsers(HttpServletRequest request) {
        try {
            checkAdmin(request);
            List<Map<String, Object>> userList = adminService.getAllUsers();
            return Result.success(userList);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分页获取用户列表（支持关键字搜索）
     * @param pageNum 页码，从 1 开始，默认 1
     * @param pageSize 每页条数，默认 10
     * @param keyword 搜索关键字（可选）
     */
    @GetMapping("/users/page")
    public Result<Map<String, Object>> getUsersPage(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            HttpServletRequest request) {
        try {
            checkAdmin(request);
            Map<String, Object> data = adminService.getUsersPage(pageNum, pageSize, keyword);
            return Result.success(data);
        } catch (Exception e) {
            log.error("分页获取用户列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 封禁/解封用户
     */
    @PostMapping("/users/{userId}/ban")
    public Result<?> banUser(
            @PathVariable Long userId,
            @RequestParam Boolean ban,
            HttpServletRequest request) {
        try {
            checkAdmin(request);
            adminService.banUser(userId, ban);
            log.info("用户状态更新: userId={}, ban={}", userId, ban);
            return Result.success();
        } catch (Exception e) {
            log.error("封禁用户失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除用户及其所有相关数据
     */
    @DeleteMapping("/users/{userId}")
    public Result<String> deleteUser(
            @PathVariable Long userId,
            HttpServletRequest request) {
        try {
            checkAdmin(request);
            adminService.deleteUser(userId);
            log.info("用户已删除: userId={}", userId);
            return Result.success("用户及其所有数据删除成功", "用户及其所有数据删除成功");
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取所有文件列表（不分页，保留兼容）
     */
    @GetMapping("/files")
    public Result<List<Map<String, Object>>> getAllFiles(HttpServletRequest request) {
        try {
            checkAdmin(request);
            List<Map<String, Object>> fileList = adminService.getAllFiles();
            return Result.success(fileList);
        } catch (Exception e) {
            log.error("获取文件列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分页获取文件列表（支持用户/文件关键字搜索）
     * @param pageNum 页码，从 1 开始，默认 1
     * @param pageSize 每页条数，默认 10
     * @param userKeyword 用户关键字（可选）
     * @param fileKeyword 文件关键字（可选）
     */
    @GetMapping("/files/page")
    public Result<Map<String, Object>> getFilesPage(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "userKeyword", required = false) String userKeyword,
            @RequestParam(value = "fileKeyword", required = false) String fileKeyword,
            HttpServletRequest request) {
        try {
            checkAdmin(request);
            Map<String, Object> data = adminService.getFilesPage(pageNum, pageSize, userKeyword, fileKeyword);
            return Result.success(data);
        } catch (Exception e) {
            log.error("分页获取文件列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除文件及其所有相关数据（管理员）
     */
    @DeleteMapping("/files/{fileId}")
    public Result<String> deleteFile(
            @PathVariable Long fileId,
            HttpServletRequest request) {
        try {
            checkAdmin(request);
            adminService.deleteFile(fileId);
            log.info("文件已删除: fileId={}", fileId);
            return Result.success("文件及其所有数据删除成功", "文件及其所有数据删除成功");
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取系统统计信息
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(HttpServletRequest request) {
        try {
            checkAdmin(request);
            Map<String, Object> stats = adminService.getStatistics();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 搜索用户（根据用户名或用户ID）
     */
    @GetMapping("/users/search")
    public Result<List<Map<String, Object>>> searchUsers(
            @RequestParam("keyword") String keyword,
            HttpServletRequest request) {
        try {
            checkAdmin(request);
            List<Map<String, Object>> userList = adminService.searchUsers(keyword);
            return Result.success(userList);
        } catch (Exception e) {
            log.error("搜索用户失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 搜索文件（根据用户信息和文件信息）
     */
    @GetMapping("/files/search")
    public Result<List<Map<String, Object>>> searchFiles(
            @RequestParam(value = "userKeyword", required = false) String userKeyword,
            @RequestParam(value = "fileKeyword", required = false) String fileKeyword,
            HttpServletRequest request) {
        try {
            checkAdmin(request);
            List<Map<String, Object>> fileList = adminService.searchFiles(userKeyword, fileKeyword);
            return Result.success(fileList);
        } catch (Exception e) {
            log.error("搜索文件失败", e);
            return Result.error(e.getMessage());
        }
    }
}

