package com.kuopan.DAO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kuopan.Entity.FileShare;import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * The table for describing the share actions Mapper 接口
 * </p>
 *
 * @author Dioxide
 * @since 2026-04-20
 */
public interface FileShareMapper extends BaseMapper<FileShare> {
    IPage<FileShare> selectByUserIdWithFileName(IPage<FileShare> page, @Param("userId") String userId);

    void updateShareShowCount(@Param("shareId") String shareId);
}
