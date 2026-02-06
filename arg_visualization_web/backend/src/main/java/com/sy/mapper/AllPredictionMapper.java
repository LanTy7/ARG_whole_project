package com.sy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sy.pojo.AllPrediction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 全部预测结果 Mapper
 */
@Mapper
public interface AllPredictionMapper extends BaseMapper<AllPrediction> {

    /**
     * 批量插入（一条 SQL 多行），用于百万级落库
     */
    void insertBatch(@Param("list") List<AllPrediction> list);
}
