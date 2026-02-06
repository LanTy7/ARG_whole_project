package com.sy.service;

import com.sy.pojo.GenomeFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 基因组文件服务接口
 */
public interface GenomeFileService {

    /**
     * 级联删除文件及其关联数据（不校验权限，供管理员/删除用户等场景调用）
     * 会删除：该文件下所有任务（及 all_predictions、class_summary、analysis_results、输出目录）、物理文件、genome_files 记录
     * @param file 文件实体，不能为 null
     */
    void deleteFileAndRelatedData(GenomeFile file);
    
    /**
     * 上传基因组文件
     * @param file 文件
     * @param userId 用户ID
     * @param options 上传选项（fileType, reference, description等）
     * @return 文件信息
     */
    Map<String, Object> uploadGenomeFile(MultipartFile file, Long userId, Map<String, Object> options);
    
    /**
     * 获取用户的基因组文件列表
     * @param userId 用户ID
     * @return 文件列表
     */
    List<Map<String, Object>> getUserGenomeFiles(Long userId);
    
    /**
     * 获取文件详细信息
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件信息
     */
    Map<String, Object> getFileInfo(Long fileId, Long userId);
    
    /**
     * 删除文件（带权限校验，用户只能删自己的文件）
     * @param fileId 文件ID
     * @param userId 用户ID
     */
    void deleteFile(Long fileId, Long userId);
    
    /**
     * 搜索文件（根据用户名、用户ID、文件ID或文件名）
     * @param keyword 搜索关键字
     * @return 文件列表
     */
    List<Map<String, Object>> searchFiles(String keyword);
}

