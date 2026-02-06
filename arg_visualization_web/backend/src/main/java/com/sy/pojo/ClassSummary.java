package com.sy.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 类别统计（对应 class_summary.tsv）
 */
@Data
@TableName("class_summary")
public class ClassSummary {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId;
    @TableField("arg_class")
    private String argClass;
    private Integer count;
}
