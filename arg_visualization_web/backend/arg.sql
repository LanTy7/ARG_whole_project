-- arg_visualization.users definition

CREATE TABLE `users` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `email` varchar(100) NOT NULL COMMENT '邮箱',
  `password` varchar(255) NOT NULL COMMENT '密码（加密后）',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `role` varchar(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER-普通用户, ADMIN-管理员',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-正常, BANNED-封禁',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_email` (`email`),
  KEY `idx_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- arg_visualization.login_logs definition

CREATE TABLE `login_logs` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `logout_time` datetime DEFAULT NULL COMMENT '退出时间',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `location` varchar(100) DEFAULT NULL COMMENT 'IP地理位置，如"浙江省杭州市"',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '浏览器信息',
  `status` varchar(20) DEFAULT NULL COMMENT '登录状态：SUCCESS-成功, FAILED-失败',
  PRIMARY KEY (`log_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志表';

-- arg_visualization.genome_files definition

CREATE TABLE `genome_files` (
  `file_id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `user_id` bigint NOT NULL COMMENT '上传用户ID',
  `original_filename` varchar(255) NOT NULL COMMENT '原始文件名',
  `stored_filename` varchar(255) NOT NULL COMMENT '存储文件名',
  `file_path` varchar(500) NOT NULL COMMENT '文件路径',
  `file_size` bigint NOT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(50) NOT NULL COMMENT '文件类型：fasta, genbank, gff等',
  `file_format` varchar(50) DEFAULT NULL COMMENT '文件格式详情',
  `md5_hash` varchar(32) DEFAULT NULL COMMENT '文件MD5值（用于去重）',
  `description` text COMMENT '文件描述',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `status` varchar(20) NOT NULL DEFAULT 'UPLOADED' COMMENT '状态：UPLOADED-已上传, ANALYZING-分析中, COMPLETED-已完成, DELETED-已删除',
  `is_public` tinyint(1) DEFAULT '0' COMMENT '是否公开：0-私有, 1-公开',
  PRIMARY KEY (`file_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_upload_time` (`upload_time`),
  KEY `idx_status` (`status`),
  KEY `idx_md5_hash` (`md5_hash`),
  CONSTRAINT `fk_genome_file_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=115 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='基因文件表';

-- arg_visualization.analysis_tasks definition

CREATE TABLE `analysis_tasks` (
  `task_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `file_id` bigint DEFAULT NULL COMMENT '基因文件ID（普通任务），MAG任务为NULL',
  `task_type` varchar(20) DEFAULT 'NORMAL' COMMENT '任务类型：NORMAL-普通任务, MAG-MAG分析',
  `mag_dir_path` varchar(500) DEFAULT NULL COMMENT 'MAG文件夹路径',
  `mag_file_count` int DEFAULT '0' COMMENT 'MAG文件数量',
  `task_name` varchar(255) DEFAULT NULL COMMENT '任务名称',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-等待中, RUNNING-运行中, COMPLETED-已完成, FAILED-失败, CANCELLED-已取消',
  `progress` int DEFAULT '0' COMMENT '进度（0-100）',
  `parameters` text COMMENT '分析参数（JSON格式）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `docker_container_id` varchar(100) DEFAULT NULL COMMENT 'Docker容器ID',
  `docker_image` varchar(255) DEFAULT NULL COMMENT 'Docker镜像名称',
  `output_dir` varchar(500) DEFAULT NULL COMMENT '输出目录',
  `error_message` text COMMENT '错误信息',
  `log_file` varchar(500) DEFAULT NULL COMMENT '日志文件路径',
  `genome_length` bigint DEFAULT NULL COMMENT '基因组长度',
  `prophage_count` int DEFAULT '0' COMMENT '识别的原噬菌体数量',
  `total_count` int DEFAULT NULL COMMENT '总序列数',
  PRIMARY KEY (`task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_task_type` (`task_type`),
  CONSTRAINT `fk_analysis_task_file` FOREIGN KEY (`file_id`) REFERENCES `genome_files` (`file_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_analysis_task_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=171 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分析任务表';

-- arg_visualization.analysis_results definition

CREATE TABLE `analysis_results` (
  `result_id` bigint NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `region_index` int NOT NULL COMMENT '区域索引（第几个原噬菌体）',
  `start_pos` int NOT NULL COMMENT '起始位置',
  `end_pos` int NOT NULL COMMENT '结束位置',
  `length` int NOT NULL COMMENT '区域长度',
  `strand` varchar(10) DEFAULT NULL COMMENT '链方向：+, -, both',
  `score` double DEFAULT NULL COMMENT '得分',
  `confidence` double DEFAULT NULL COMMENT '置信度（0-1）',
  `completeness` varchar(20) DEFAULT NULL COMMENT '完整性：complete-完整, incomplete-不完整, questionable-可疑',
  `gene_count` int DEFAULT '0' COMMENT '基因数量',
  `genes` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '基因列表（JSON格式）',
  `functional_category` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '功能分类统计（JSON格式）',
  `annotations` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '注释信息（JSON格式）',
  `gc_content` double DEFAULT NULL COMMENT 'GC含量',
  PRIMARY KEY (`result_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_region_index` (`region_index`),
  KEY `idx_score` (`score`),
  CONSTRAINT `fk_analysis_result_task` FOREIGN KEY (`task_id`) REFERENCES `analysis_tasks` (`task_id`) ON DELETE CASCADE,
  CONSTRAINT `analysis_results_chk_1` CHECK (json_valid(`genes`)),
  CONSTRAINT `analysis_results_chk_2` CHECK (json_valid(`functional_category`)),
  CONSTRAINT `analysis_results_chk_3` CHECK (json_valid(`annotations`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分析结果表-原噬菌体区域';

-- arg_visualization.all_predictions definition

CREATE TABLE `all_predictions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL COMMENT '分析任务ID',
  `row_index` int NOT NULL COMMENT '行号(1-based)',
  `sequence_id` varchar(2048) NOT NULL DEFAULT '' COMMENT '序列ID',
  `is_arg` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否抗性基因 1=是 0=否',
  `binary_prob` double DEFAULT NULL COMMENT '二分类概率',
  `arg_class` varchar(256) DEFAULT NULL COMMENT 'ARG类别',
  `class_prob` double DEFAULT NULL COMMENT '类别概率',
  PRIMARY KEY (`id`),
  KEY `idx_task_row` (`task_id`,`row_index`),
  KEY `idx_task_is_arg` (`task_id`,`is_arg`)
) ENGINE=InnoDB AUTO_INCREMENT=42261 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='全部预测结果';

-- arg_visualization.class_summary definition

CREATE TABLE `class_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL COMMENT '分析任务ID',
  `arg_class` varchar(256) NOT NULL COMMENT '类别名',
  `count` int NOT NULL COMMENT '该类别数量',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB AUTO_INCREMENT=82 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='类别统计';

--insert data into users table
INSERT INTO `users` VALUES
(1,'admin','admin@provirus.com','$2a$10$n6yncgdbw3/E4uc1c6iMsu/ce2.Uw1gnaalC7GHKqhv/PDNMpyuuW','系统管理员',NULL,'ADMIN','ACTIVE','2026-01-08 10:24:10','2026-01-08 10:24:10','2026-02-01 14:41:35'),
(2,'lanty','277862362@qq.com','$2a$10$GtOm68uG0mu4Dcd1g9PGze2ySkaRZ94nOXcF8/c9DhsJuNBAepzxC',NULL,'https://api.dicebear.com/7.x/avataaars/svg?seed=lanty','ADMIN','ACTIVE','2026-01-08 21:55:29','2026-01-08 13:55:29','2026-01-09 09:12:22');
