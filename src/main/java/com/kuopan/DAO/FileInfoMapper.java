package com.kuopan.DAO;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.kuopan.Entity.FileInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    // Query the file with FileID and UserID
    FileInfo selectByFileIDAndUserID(@Param("fileId") String fileId,@Param("userId") String userId);

    // Update the status with old one (the Optimistic Lock)
    void updateFileStatusWithOldStatus(@Param("fileId") String fileId,@Param("userId") String userId,
                                       @Param("bean") Object t, @Param("oldStatus") Integer oldStatus);


    // Query the file list with the name
    IPage<FileInfo> selectFileWithUser(IPage<FileInfo> page, @Param(Constants.WRAPPER) LambdaQueryWrapper<FileInfo> query);
}
