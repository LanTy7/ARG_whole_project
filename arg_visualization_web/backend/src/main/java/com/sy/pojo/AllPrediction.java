package com.sy.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 全部预测结果（对应 all_predictions.tsv）
 */
@Data
@TableName("all_predictions")
public class AllPrediction {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId;
    @TableField("row_index")
    private Integer rowIndex;
    @TableField("sequence_id")
    private String sequenceId;
    @TableField("is_arg")
    private Boolean isArg;
    @TableField("binary_prob")
    private Double binaryProb;
    @TableField("arg_class")
    private String argClass;
    @TableField("class_prob")
    private Double classProb;
}
