package com.sy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sy.pojo.ClassSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 类别统计 Mapper
 */
@Mapper
public interface ClassSummaryMapper extends BaseMapper<ClassSummary> {

    /**
     * 批量插入（一条 SQL 多行）
     */
    void insertBatch(@Param("list") List<ClassSummary> list);
}
