package com.kuopan.Service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuopan.DAO.FileShareMapper;
import com.kuopan.Entity.FileInfo;
import com.kuopan.Entity.FileShare;
import com.kuopan.Entity.constants.Constants;
import com.kuopan.Entity.dto.SessionShareDto;
import com.kuopan.Entity.enums.ResponseCodeEnum;
import com.kuopan.Entity.enums.ShareValidTypeEnum;
import com.kuopan.Exception.BusinessException;
import com.kuopan.Service.IFileShareService;
import com.kuopan.Util.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * The table for describing the share actions 服务实现类
 * </p>
 *
 * @author Dioxide
 * @since 2026-04-20
 */
@Service
public class FileShareServiceImpl extends ServiceImpl<FileShareMapper, FileShare> implements IFileShareService {

    @Resource
    private FileShareMapper fileShareMapper;

    @Override
    public IPage<FileShare> findListByDescInShareTime(Integer pageNo, Integer pageSize, String userId) {
        IPage<FileShare> page = new Page<>(pageNo, pageSize);
        fileShareMapper.selectByUserIdWithFileName(page, userId);
        return page;
    }

    @Override
    public void saveShare(FileShare fileShare) {
        ShareValidTypeEnum typeEnum = ShareValidTypeEnum.getByType(fileShare.getValidType());
        if (typeEnum == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (!ShareValidTypeEnum.FOREVER.equals(typeEnum)) {
            fileShare.setExpireTime(LocalDateTime.now().plusDays(typeEnum.getDays()));
        }
        LocalDateTime curDate = LocalDateTime.now();
        fileShare.setShareTime(curDate);
        if (StringUtil.isEmpty(fileShare.getCode())) {
            fileShare.setCode(StringUtil.getRandomString(Constants.FIVE));
        }
        fileShare.setShareId(StringUtil.getRandomString(Constants.TWENTY));
        fileShareMapper.insert(fileShare);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFileShareBatch(String[] shareIdArray, String userId) {
        Integer count = fileShareMapper.delete(new LambdaQueryWrapper<FileShare>()
                .eq(!StringUtil.isEmpty(userId), FileShare::getUserId, userId)
                .in(FileShare::getShareId, shareIdArray));
        if (count != shareIdArray.length) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    @Override
    public SessionShareDto checkShareCode(String shareId, String code) {
        // checksum
        FileShare fileShare = fileShareMapper.selectById(shareId);
        if (fileShare == null || (fileShare.getExpireTime() != null && LocalDateTime.now().isAfter(fileShare.getExpireTime()))) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }

        if (!fileShare.getCode().equals(code)) {
            throw new BusinessException("提取码错误");
        }

        // Update browsing count.
        fileShareMapper.updateShareShowCount(shareId);
        SessionShareDto shareDto = new SessionShareDto();
        shareDto.setShareId(shareId)
                .setShareUserId(fileShare.getUserId())
                .setFileId(fileShare.getFileId())
                .setExpireTime(fileShare.getExpireTime());
        return shareDto;
    }


}
