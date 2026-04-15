package com.kuopan.Service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;import com.baomidou.mybatisplus.core.metadata.IPage;import com.baomidou.mybatisplus.extension.plugins.pagination.Page;import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuopan.Component.RedisComponent;
import com.kuopan.DAO.FileInfoMapper;
import com.kuopan.Entity.FileInfo;
import com.kuopan.Entity.constants.Constants;
import com.kuopan.Entity.dto.SessionWebUserDto;
import com.kuopan.Entity.dto.UploadResultDto;
import com.kuopan.Entity.dto.UserSpaceDto;
import com.kuopan.Entity.enums.*;
import com.kuopan.Exception.BusinessException;
import com.kuopan.Service.IFileInfoService;
import com.kuopan.Util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * The file detailed information. 服务实现类
 * </p>
 *
 * @author Dioxide
 * @since 2026-04-14
 */
@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements IFileInfoService {

    @Resource
    private RedisComponent redisComponent;
    @Autowired
    private FileInfoMapper fileInfoMapper;


    @Override
    public IPage<FileInfo> findListByDescInUse(Integer pageNo, Integer pageSize, String userId, String category) {
        IPage<FileInfo> page = new Page<>(pageNo, pageSize);
        FileCategoryEnum Enum = FileCategoryEnum.getByCode(category);

        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(Enum != null, FileInfo::getFileCategory, Enum != null ? Enum.getCategory() : null);

        queryWrapper.eq(FileInfo::getUserId, userId);

        queryWrapper.orderByDesc(FileInfo::getLastUpdateTime);

        queryWrapper.eq(FileInfo::getDelFlag, FileDelFlag.USING.getFlag());

        return this.page(page, queryWrapper);
    }

    @Override
    public UploadResultDto uploadFile(SessionWebUserDto userDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks) {
        UploadResultDto resultDto = new UploadResultDto();
        if (StringUtil.isEmpty(fileId)) {
            fileId = StringUtil.getRandomNumber(Constants.TEN);
        }
        resultDto.setFileId(fileId);
        LocalDateTime currentDate = LocalDateTime.now();
        UserSpaceDto spaceDto = redisComponent.getUserUsedSpace(userDto.getUserId());

        if (chunkIndex == 0) {
            LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileInfo::getFileMd5, fileMd5)
                        .eq(FileInfo::getStatus, FileInfoStatus.USING.getStatus());
            List<FileInfo> fileList = fileInfoMapper.selectList(queryWrapper);
            // Fast upload
            if (!fileList.isEmpty()) {
                FileInfo dbFile = fileList.get(0);
                // Size confirmation
                if (dbFile.getFileSize() + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
                    throw new BusinessException(ResponseCodeEnum.CODE_904);
                }
                // Rename
                fileName = autoRename(filePid, userDto.getUserId(), fileName);

                dbFile.setFileId(fileId)
                        .setFilePid(filePid)
                        .setUserId(userDto.getUserId())
                        .setCreateTime(currentDate)
                        .setLastUpdateTime(currentDate)
                        .setStatus(FileInfoStatus.USING.getStatus())
                        .setFileMd5(fileMd5)
                        .setFileName(fileName);
                resultDto.setStatus(UploadStatusEnum.FLASH_UPLOAD.getCode());
                // Update the user used space

            }
        }
        return resultDto;
    }

    private String autoRename(String filePid, String userId, String fileName) {
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getFilePid, filePid)
                .eq(FileInfo::getDelFlag, FileDelFlag.USING.getFlag())
                .eq(FileInfo::getFileName, fileName);
        Integer count = Math.toIntExact(fileInfoMapper.selectCount(queryWrapper));
        if (count > 0) {
            fileName = StringUtil.rename(fileName);
        }

        return fileName;
    }

    private void updateUserSpace(SessionWebUserDto userDto, Long totalSize) {

    }
}
