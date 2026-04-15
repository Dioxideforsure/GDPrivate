package com.kuopan.DAO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuopan.Entity.FileInfo;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * The file detailed information. Mapper 接口
 * </p>
 *
 * @author Dioxide
 * @since 2026-04-14
 */
public interface FileInfoMapper extends BaseMapper<FileInfo> {
    // Query the total space from XML.
    Long selectUsedSpace(@Param("userId") String userId);
}
