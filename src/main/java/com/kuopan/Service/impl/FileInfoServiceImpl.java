package com.kuopan.Service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuopan.Component.RedisComponent;
import com.kuopan.Config.AppConfig;
import com.kuopan.DAO.FileInfoMapper;
import com.kuopan.DAO.FileShareMapper;
import com.kuopan.DAO.UserInfoMapper;
import com.kuopan.Entity.FileInfo;
import com.kuopan.Entity.FileShare;
import com.kuopan.Entity.UserInfo;
import com.kuopan.Entity.constants.Constants;
import com.kuopan.Entity.dto.SessionWebUserDto;
import com.kuopan.Entity.dto.UploadResultDto;
import com.kuopan.Entity.dto.UserSpaceDto;
import com.kuopan.Entity.enums.*;
import com.kuopan.Exception.BusinessException;
import com.kuopan.Service.IFileInfoService;
import com.kuopan.Service.IFileShareService;
import com.kuopan.Util.FfmpegUtils;
import com.kuopan.Util.ScaleFilter;
import com.kuopan.Util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * The file detailed information. 服务实现类
 * </p>
 *
 * @author Dioxide
 * @since 2026-04-14
 */
@Service
@Slf4j
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements IFileInfoService {

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Resource
    private IFileShareService fileShareService;

    @Resource
    private AppConfig appConfig;

    @Resource
    @Lazy
    private FileInfoServiceImpl fileInfoService;

    @Override
    public IPage<FileInfo> findListByDescInUse(Integer pageNo, Integer pageSize, String userId, String category, String filePid, String fileNameFuzzy) {

        IPage<FileInfo> page = new Page<>(pageNo, pageSize);
        FileCategoryEnum Enum = FileCategoryEnum.getByCode(category);

        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(Enum != null, FileInfo::getFileCategory, Enum != null ? Enum.getCategory() : null);

        queryWrapper.eq(FileInfo::getUserId, userId)
                .eq(!StringUtil.isEmpty(filePid), FileInfo::getFilePid, filePid)
                .like(!StringUtil.isEmpty(fileNameFuzzy), FileInfo::getFileName, fileNameFuzzy);

        queryWrapper.orderByDesc(FileInfo::getLastUpdateTime);

        queryWrapper.eq(FileInfo::getDelFlag, FileDelFlag.USING.getFlag());

        return this.page(page, queryWrapper);
    }

    @Override
    public IPage<FileInfo> findListByDesc4Recycle(Integer pageNo, Integer pageSize, String userId) {

        IPage<FileInfo> page = new Page<>(pageNo, pageSize);

        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(FileInfo::getUserId, userId)
                .orderByDesc(FileInfo::getRecoveryTime)
                .eq(FileInfo::getDelFlag, FileDelFlag.RECYCLE.getFlag());

        return this.page(page, queryWrapper);
    }

    // Database will check the availability of update, if failed the transaction will roll back.
    @Transactional(rollbackFor = Exception.class)
    @Override
    public UploadResultDto uploadFile(SessionWebUserDto userDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks) {
        UploadResultDto resultDto = new UploadResultDto();
        Boolean uploadSuccess = true;
        File tempFileFolder = null;
        try {
            if (StringUtil.isEmpty(fileId)) {
                fileId = StringUtil.getRandomString(Constants.TEN);
            }
            resultDto.setFileId(fileId);
            LocalDateTime currentDate = LocalDateTime.now();
            UserSpaceDto spaceDto = redisComponent.getUserUsedSpace(userDto.getUserId());

            if (chunkIndex == 0) {
                LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FileInfo::getFileMd5, fileMd5).eq(FileInfo::getStatus, FileInfoStatus.USING.getStatus()).last("limit 1");
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

                    dbFile.setFileId(fileId).setFilePid(filePid).setUserId(userDto.getUserId()).setCreateTime(currentDate).setLastUpdateTime(currentDate).setStatus(FileInfoStatus.USING.getStatus()).setFileMd5(fileMd5).setFileName(fileName).setDelFlag(FileDelFlag.USING.getFlag());

                    // Update the database
                    fileInfoMapper.insert(dbFile);

                    resultDto.setStatus(UploadStatusEnum.FLASH_UPLOAD.getCode());
                    // Update the user used space
                    updateUserSpace(userDto, dbFile.getFileSize());
                    return resultDto;
                }
            }
            // Disk Space Confirmation

            Long currentTempSize = redisComponent.getFileTempSize(userDto.getUserId(), fileId);

            if (file.getSize() + currentTempSize + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
                throw new BusinessException(ResponseCodeEnum.CODE_904);
            }

            // Buffer the temp directory
            String tempFolderName = appConfig.getProjectFolder() + Constants.TEMP_FOLDER_FILE;
            String currentUserFolderName = userDto.getUserId() + fileId;
            tempFileFolder = new File(tempFolderName + currentUserFolderName);
            if (!tempFileFolder.exists()) {
                tempFileFolder.mkdirs();
            }

            // Upload business
            File newFile = new File(tempFileFolder.getPath() + "/" + chunkIndex);
            file.transferTo(newFile);

            redisComponent.saveFileTempSize(userDto.getUserId(), fileId, file.getSize());
            if (chunkIndex < chunks - 1) {
                resultDto.setStatus(UploadStatusEnum.UPLOADING.getCode());
                return resultDto;
            }

            redisComponent.saveFileTempSize(userDto.getUserId(), fileId, file.getSize());

            // Last piece upload completes, database in record, combine in async.
            String month = YearMonth.now().format(Constants.standard_yyyyMM);
            String fileSuffix = StringUtil.getFileNameSuffix(fileName);
            String fileRealName = currentUserFolderName + fileSuffix;
            FileTypeEnum fileTypeEnum = FileTypeEnum.getFileTypeBySuffix(fileSuffix);
            // Auto rename
            fileName = autoRename(filePid, userDto.getUserId(), fileName);

            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileId(fileId).setUserId(userDto.getUserId()).setFileMd5(fileMd5).setFileName(fileName).setFilePath(month + "/" + fileRealName).setFilePid(filePid).setCreateTime(currentDate).setLastUpdateTime(currentDate).setFileCategory(fileTypeEnum.getCategory().getCategory()).setFileType(fileTypeEnum.getType()).setStatus(FileInfoStatus.TRANSFORM.getStatus()).setFolderType(FileFolderTypeEnum.FILE.getType()).setDelFlag(FileDelFlag.USING.getFlag());

            fileInfoMapper.insert(fileInfo);

            Long totalSize = redisComponent.getFileTempSize(userDto.getUserId(), fileId);
            updateUserSpace(userDto, totalSize);

            resultDto.setStatus(UploadStatusEnum.UPLOADED.getCode());

            // Combine the file in async operation
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileInfoService.transformFile(fileInfo.getFileId(), userDto);
                }
            });


            return resultDto;
        } catch (BusinessException e) {
            log.error("文件上传失败", e);
            throw e;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            uploadSuccess = false;
        } finally {
            if (!uploadSuccess && tempFileFolder == null) {
                try {
                    FileUtils.deleteDirectory(tempFileFolder);
                } catch (IOException e) {
                    log.error("删除临时目录失败", e);
                }
            }
        }

        return resultDto;
    }

    @Override
    public FileInfo getFileInfoByUserIdAndFileId(String userId, String fileId) {
        FileInfo fileInfo = fileInfoMapper.selectOne(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getFileId, fileId)
                .eq(FileInfo::getUserId, userId));
        return fileInfo;
    }

    @Override
    public FileInfo newFolder(String filePid, String userId, String folderName) {
        checkFileName(filePid, userId, folderName, FileFolderTypeEnum.FOLDER.getType());
        LocalDateTime curdate = LocalDateTime.now();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileId(StringUtil.getRandomString(Constants.TEN)).setUserId(userId).setFilePid(filePid).setFileName(folderName).setFolderType(FileFolderTypeEnum.FOLDER.getType()).setCreateTime(curdate).setLastUpdateTime(curdate).setStatus(FileInfoStatus.USING.getStatus()).setDelFlag(FileDelFlag.USING.getFlag());
        fileInfoMapper.insert(fileInfo);
        return fileInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo rename(String fileId, String userId, String fileName) {
        FileInfo fileInfo = fileInfoMapper.selectOne(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getFileId, fileId)
                .eq(FileInfo::getUserId, userId));
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }
        String filePid = fileInfo.getFilePid();
        checkFileName(filePid, userId, fileName, fileInfo.getFolderType());

        // Suffix
        if (FileFolderTypeEnum.FILE.getType().equals(fileInfo.getFolderType())) {
            fileName = fileName + StringUtil.getFileNameSuffix(fileInfo.getFileName());
        }
        LocalDateTime curDate = LocalDateTime.now();
        FileInfo dbInfo = new FileInfo();
        dbInfo.setFileName(fileName).setLastUpdateTime(curDate);
        LambdaUpdateWrapper<FileInfo> updateWrapper = new LambdaUpdateWrapper();
        updateWrapper.eq(FileInfo::getFileId, fileId).eq(FileInfo::getUserId, userId);
        fileInfoMapper.update(dbInfo, updateWrapper);

        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(!StringUtil.isEmpty(fileId), FileInfo::getFileId, fileId)
                .eq(FileInfo::getUserId, userId)
                .eq(!StringUtil.isEmpty(filePid), FileInfo::getFilePid, filePid)
                .eq(FileInfo::getDelFlag, FileDelFlag.USING.getFlag());
        Integer count = fileInfoMapper.selectCount(queryWrapper).intValue();
        if (count > 1) {
            throw new BusinessException("文件名" + fileName + "已经存在");
        }
        fileInfo.setFileName(fileName).setLastUpdateTime(curDate);
        return fileInfo;

    }

    @Override
    public List<FileInfo> loadAllFolder(String userId, String filePid, String currentFileIds) {
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getFilePid, filePid)
                .eq(FileInfo::getFolderType, FileFolderTypeEnum.FOLDER.getType());
        if (!StringUtil.isEmpty(currentFileIds)) {
            queryWrapper.notIn(FileInfo::getFileId, currentFileIds.split(","));
        }
        queryWrapper.eq(FileInfo::getDelFlag, FileDelFlag.USING.getFlag())
                .orderByDesc(FileInfo::getCreateTime);
        return fileInfoMapper.selectList(queryWrapper);
    }

    @Override
    public void changeFileFolder(String fileIds, String filePid, String userId) {
        if (fileIds.equals(filePid)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (!Constants.ZERO_STRING.equals(filePid)) {
            FileInfo fileInfo = fileInfoMapper.selectOne(new LambdaQueryWrapper<FileInfo>()
                    .eq(FileInfo::getFileId, filePid) // fileId is the one needed changed
                    .eq(FileInfo::getUserId, userId));
            if (fileInfo == null || !FileDelFlag.USING.getFlag().equals(fileInfo.getDelFlag())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }
        String[] fileIdArray = fileIds.split(",");

        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getFilePid, filePid)
                .eq(FileInfo::getUserId, userId);

        List<FileInfo> dbFileList = fileInfoMapper.selectList(queryWrapper);
        Map<String, FileInfo> dbFileNameMap = dbFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));

        // query the selected file
        LambdaQueryWrapper<FileInfo> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(FileInfo::getUserId, userId).in(FileInfo::getFileId, fileIdArray);
        List<FileInfo> selectFileList = fileInfoMapper.selectList(queryWrapper1);

        // Rename selected file
        for (int i = 0; i < selectFileList.size(); i++) {
            FileInfo rootFileInfo = dbFileNameMap.get(selectFileList.get(i).getFileName());
            // Filename duplicates, rename should roll back.
            FileInfo updateInfo = new FileInfo();
            if (rootFileInfo != null) {
                String fileName = StringUtil.rename(selectFileList.get(i).getFileName());
                updateInfo.setFileName(fileName);
            }
            updateInfo.setFilePid(filePid);
            fileInfoMapper.update(updateInfo, new LambdaUpdateWrapper<FileInfo>().eq(FileInfo::getFileId, selectFileList.get(i).getFileId()).eq(FileInfo::getUserId, userId));

        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFile2RecycleBatch(String userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getUserId, userId)
                .in(FileInfo::getFileId, fileIdArray)
                .eq(FileInfo::getDelFlag, FileDelFlag.USING.getFlag());
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(queryWrapper);
        if (fileInfoList.isEmpty()) {
            return;
        }
        List<String> delFilePidList = new ArrayList<>();
        for (int i = 0; i < fileInfoList.size(); i++) {
            findAllSubFolderFileList(delFilePidList, userId, fileInfoList.get(i).getFileId(), FileDelFlag.USING.getFlag());
        }
        if (!delFilePidList.isEmpty()) {
            FileInfo updateInfo = new FileInfo();
            updateInfo.setDelFlag(FileDelFlag.DEL.getFlag());
            updateFileDelFlagBatch(updateInfo, userId, delFilePidList, null, FileDelFlag.USING.getFlag());
        }
        // Selected Files move to recycle bin
        List<String> delFileIdList = Arrays.asList(fileIdArray);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setRecoveryTime(LocalDateTime.now())
                .setDelFlag(FileDelFlag.RECYCLE.getFlag());
        updateFileDelFlagBatch(fileInfo, userId, null, delFileIdList, FileDelFlag.USING.getFlag());

        // Share database should update
        fileShareService.remove(new LambdaQueryWrapper<FileShare>()
                .eq(!StringUtil.isEmpty(userId), FileShare::getUserId, userId)
                .in(!(delFileIdList == null || delFileIdList.isEmpty()), FileShare::getFileId, delFileIdList)
                .in(!(delFilePidList == null || delFilePidList.isEmpty()), FileShare::getFileId, delFilePidList));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverFileBatch(String userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getUserId, userId)
                .in(FileInfo::getFileId, fileIdArray)
                .eq(FileInfo::getDelFlag, FileDelFlag.RECYCLE.getFlag());
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(queryWrapper);
        List<String> delFileSubFolderFileIdList = new ArrayList<>();
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnum.FOLDER.getType().equals(fileInfo.getFolderType())) {
                findAllSubFolderFileList(delFileSubFolderFileIdList, userId, fileInfo.getFileId(), FileDelFlag.DEL.getFlag());

            }
        }
        // Query the root file
        LambdaQueryWrapper<FileInfo> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getDelFlag, FileDelFlag.USING.getFlag())
                .eq(FileInfo::getFilePid, Constants.ZERO_STRING);
        List<FileInfo> allRootFileList = fileInfoMapper.selectList(queryWrapper1);

        Map<String, FileInfo> rootFileMap = allRootFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (data1, data2) -> data2));

        // Query selected files, update all the flag to using from the deleted category
        if (!delFileSubFolderFileIdList.isEmpty()) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setDelFlag(FileDelFlag.USING.getFlag());
            updateFileDelFlagBatch(fileInfo, userId, delFileSubFolderFileIdList, null, FileDelFlag.DEL.getFlag());
        }

        // Update the file del flag to normal, and parent category to the root.
        List<String> delFileIdList = Arrays.asList(fileIdArray);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setDelFlag(FileDelFlag.USING.getFlag())
                .setFilePid(Constants.ZERO_STRING)
                .setLastUpdateTime(LocalDateTime.now());
        updateFileDelFlagBatch(fileInfo, userId, null, delFileIdList, FileDelFlag.RECYCLE.getFlag());

        // Rename their name
        for (FileInfo item : fileInfoList) {
            FileInfo rootFileInfo = rootFileMap.get(item.getFileName());
            // File name exists, rename to a new name
            if (rootFileInfo != null) {
                String fileName = StringUtil.rename(item.getFileName());
                FileInfo updateInfo = new FileInfo();
                updateInfo.setFileName(fileName);
                fileInfoMapper.update(updateInfo, new LambdaUpdateWrapper<FileInfo>().eq(FileInfo::getFileId, item.getFileId()).eq(FileInfo::getUserId, userId));
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delFileBatch(String userId, String fileIds, Boolean adminOp) {
        String[] fileIdArray = fileIds.split(",");
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getUserId, userId)
                .in(FileInfo::getFileId, fileIdArray)
                .eq(FileInfo::getDelFlag, FileDelFlag.RECYCLE.getFlag());
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(queryWrapper);

        List<String> delFileSubFileFolderFileIdList = new ArrayList<>();
        // Find the sub id of the parent folder
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnum.FOLDER.getType().equals(fileInfo.getFolderType())) {
                findAllSubFolderFileList(delFileSubFileFolderFileIdList, userId, fileInfo.getFileId(), FileDelFlag.DEL.getFlag());
            }
        }
        // Delete all the selected including their sub folders or files
        if (!delFileSubFileFolderFileIdList.isEmpty()) {
            fileInfoMapper.delete(new LambdaQueryWrapper<FileInfo>()
                    .eq(FileInfo::getUserId, userId)
                    .in(FileInfo::getFilePid, delFileSubFileFolderFileIdList)
                    .eq(!adminOp, FileInfo::getDelFlag, FileDelFlag.DEL.getFlag()));
        }
        // Delete the selected files
        fileInfoMapper.delete(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getUserId, userId)
                .in(FileInfo::getFileId, Arrays.asList(fileIdArray))
                .eq(!adminOp, FileInfo::getDelFlag, FileDelFlag.RECYCLE.getFlag()));

        Long usedSpace = fileInfoMapper.selectUsedSpace(userId);
        UserInfo userInfo = new UserInfo();
        userInfo.setOccuSpace(usedSpace);
        userInfoMapper.update(userInfo, new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUserId, userId));

        // Cache setting
        UserSpaceDto userSpaceDto = redisComponent.getUserUsedSpace(userId);
        userSpaceDto.setUseSpace(usedSpace);
        redisComponent.saveUserUsedSpace(userId, userSpaceDto);
    }

    @Override
    public IPage<FileInfo> findAllListByDescInUse(Integer pageNo, Integer pageSize, String filePid, String fileNameFuzzy) {
        IPage<FileInfo> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(!StringUtil.isEmpty(fileNameFuzzy), FileInfo::getFileName, fileNameFuzzy)
                .eq(!StringUtil.isEmpty(filePid), FileInfo::getFilePid, filePid);
        fileInfoMapper.selectFileWithUser(page, queryWrapper);
        return page;
    }

    /**
     *
     *
     * Methods to implement the public one.
     *
     *
     *
     */

    private void checkFileName(String filePid, String userId, String fileName, Boolean folderType) {
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getFolderType, folderType)
                .eq(FileInfo::getFileName, fileName)
                .eq(FileInfo::getFilePid, filePid)
                .eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getDelFlag, FileDelFlag.USING.getFlag());
        Integer count = Math.toIntExact(fileInfoMapper.selectCount(queryWrapper));
        if (count > 0) {
            throw new BusinessException("此目录下存在同一文件，请修改名称");
        }
    }

    private String autoRename(String filePid, String userId, String fileName) {
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getUserId, userId).eq(FileInfo::getFilePid, filePid).eq(FileInfo::getDelFlag, FileDelFlag.USING.getFlag()).eq(FileInfo::getFileName, fileName);
        Integer count = Math.toIntExact(fileInfoMapper.selectCount(queryWrapper));
        if (count > 0) {
            fileName = StringUtil.rename(fileName);
        }

        return fileName;
    }

    private void updateUserSpace(SessionWebUserDto userDto, Long usedSpace) {
        Integer count = userInfoMapper.updateRemainSpaceByGUID(userDto.getUserId(), usedSpace, null);
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_904);
        }
        UserSpaceDto spaceDto = redisComponent.getUserUsedSpace(userDto.getUserId());
        spaceDto.setUseSpace(spaceDto.getUseSpace() + usedSpace);
        redisComponent.saveUserUsedSpace(userDto.getUserId(), spaceDto);

    }

    @Async
    public void transformFile(String fileId, SessionWebUserDto userDto) {
        Boolean transformSuccess = true;
        String targetFilePath = null;
        String cover = null;
        FileTypeEnum fileTypeEnum = null;
        FileInfo fileInfo = fileInfoMapper.selectOne(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getFileId, fileId).eq(FileInfo::getUserId, userDto.getUserId()));
        try {
            if (fileInfo == null || !FileInfoStatus.TRANSFORM.getStatus().equals(fileInfo.getStatus())) {
                return;
            }
            // Temp folder
            String tempFolderName = appConfig.getProjectFolder() + Constants.TEMP_FOLDER_FILE;
            String currentUserFolderName = userDto.getUserId() + fileId;
            File fileFolder = new File(tempFolderName + currentUserFolderName);

            // Access the created month and suffix
            String fileSuffix = StringUtil.getFileNameSuffix(fileInfo.getFileName());
            String month = fileInfo.getCreateTime().format(Constants.standard_yyyyMM);

            // Target Folder
            String targetFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            File targetFolder = new File(targetFolderName + "/" + month);
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }

            // Real File Name
            String realFileName = currentUserFolderName + fileSuffix;
            targetFilePath = targetFolder.getPath() + "/" + realFileName;


            // Combine the file.
            combine(fileFolder.getPath(), targetFilePath, fileInfo.getFileName(), true);

            // Shred the video file.
            // Video files cut
            fileTypeEnum = FileTypeEnum.getFileTypeBySuffix(fileSuffix);
            if (fileTypeEnum == FileTypeEnum.VIDEO) {
                cutFile4Video(fileId, targetFilePath);
                // Video cover generate
                cover = month + "/" + currentUserFolderName + Constants.IMAGE_PNG_SUFFIX;
                String coverPath = targetFolderName + "/" + cover;
                ScaleFilter.createCover4Video(new File(targetFilePath), Constants.ONE_FIFTY, new File(coverPath));
            } else if (fileTypeEnum == FileTypeEnum.IMAGE) {
                // Picture cover generate
                cover = month + "/" + realFileName.replace(".", "_.");
                String coverPath = targetFolderName + "/" + cover;
                Boolean created = ScaleFilter.createThumbnailWidthFFmpeg(new File(targetFilePath), Constants.ONE_FIFTY, new File(coverPath), false);
                if (!created) {
                    FileUtils.copyFile(new File(targetFilePath), new File(coverPath));
                }
            }
        } catch (Exception e) {
            log.error("文件转码失败", "文件ID:{}, 用户ID:{}", fileId, userDto.getUserId(), e);
            transformSuccess = false;
        } finally {
            FileInfo updateInfo = new FileInfo();
            updateInfo.setFileSize(new File(targetFilePath).length()).setFileCover(cover).setStatus(transformSuccess ? FileInfoStatus.USING.getStatus() : FileInfoStatus.TRANSFORM_FAIL.getStatus());
            fileInfoMapper.updateFileStatusWithOldStatus(fileId, userDto.getUserId(), updateInfo, FileInfoStatus.TRANSFORM.getStatus());

        }
    }

    private void combine(String dirPath, String toFilePath, String fileName, Boolean delSource) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new BusinessException("文件不存在");
        }
        File[] fileList = dir.listFiles();
        File targetFile = new File(toFilePath);
        RandomAccessFile writeFile = null;
        try {
            writeFile = new RandomAccessFile(targetFile, "rw");
            byte[] buffer = new byte[1024 * 10];
            for (int i = 0; i < fileList.length; i++) {
                int len = -1;
                File chunkFile = new File(dirPath + "/" + i);
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile, "r");
                    while ((len = readFile.read(buffer)) != -1) {
                        writeFile.write(buffer, 0, len);
                    }
                } catch (Exception e) {
                    log.error("合并分片失败", e);
                    throw new BusinessException("合并文件失败");
                } finally {
                    readFile.close();
                }

            }
        } catch (Exception e) {
            log.error("合并文件失败", fileName, e);
            throw new BusinessException("合并文件" + fileName + "出错了");
        } finally {
            if (writeFile == null) {
                try {
                    writeFile.close();
                } catch (IOException e) {
                    log.error("合并文件失败" + e);
                    throw new BusinessException("合并文件失败");
                }
            }
            if (delSource && dir.exists()) {
                try {
                    FileUtils.deleteDirectory(dir);
                } catch (IOException e) {
                    log.error("合并文件失败" + e);
                    throw new BusinessException("合并文件失败");
                }
            }
        }
    }

    private void cutFile4Video(String fileId, String videoFilePath) {
        // Create the folder for sharding
        File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf('.')));
        if (!tsFolder.exists()) {
            tsFolder.mkdirs();
        }
        final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s -c copy %s";
        final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";
        String tsPath = tsFolder + "/" + Constants.TS_NAME;

        // Generate
        String cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
        FfmpegUtils.executeCommand(cmd, false);

        // Index file .m3u8 and sharding .ts
        cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath(), fileId);
        FfmpegUtils.executeCommand(cmd, false);

        // Delete index file
        new File(tsPath).delete();
    }

    private void findAllSubFolderFileList(List<String> fileIdList, String userId, String fileId, Integer delFlag) {
        fileIdList.add(fileId);
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getUserId, userId).eq(FileInfo::getFilePid, fileId).eq(FileInfo::getDelFlag, delFlag).eq(FileInfo::getFolderType, FileFolderTypeEnum.FOLDER.getType());
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(queryWrapper);
        for (int i = 0; i < fileInfoList.size(); i++) {
            findAllSubFolderFileList(fileIdList, userId, fileInfoList.get(i).getFileId(), delFlag);
        }

    }

    private void updateFileDelFlagBatch(FileInfo fileInfo, String userId, List<String> filePidList, List<String> fileIdList, Integer oldDelFlag) {
        LambdaUpdateWrapper<FileInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FileInfo::getUserId, userId);

        if (filePidList != null && !filePidList.isEmpty()) {
            updateWrapper.in(FileInfo::getFilePid, filePidList);
        }
        if (fileIdList != null && !fileIdList.isEmpty()) {
            updateWrapper.in(FileInfo::getFileId, fileIdList);
        }
        if (oldDelFlag != null) {
            updateWrapper.eq(FileInfo::getDelFlag, oldDelFlag);
        }

        fileInfoMapper.update(fileInfo, updateWrapper);
    }

    @Override
    public void checkRootFilePid(String filePid, String userId, String fileId) {
        if (StringUtil.isEmpty(fileId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (filePid.equals(fileId)) {
            return;
        }
        checkFilePid(filePid, userId, fileId);
    }

    @Override
    public IPage<FileInfo> findFileInShareFolder(String filePid, String fileId, String userId, Integer pageNo, Integer pageSize) {
        IPage<FileInfo> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(!StringUtil.isEmpty(fileId), FileInfo::getFileId, fileId)
                .eq(FileInfo::getUserId, userId)
                .eq(!StringUtil.isEmpty(filePid), FileInfo::getFilePid, filePid)
                .eq(FileInfo::getDelFlag, FileDelFlag.USING.getFlag())
                .orderByDesc(FileInfo::getLastUpdateTime);

        return page(page, queryWrapper);
    }

    @Override
    public void saveShare(String shareRootFilePid, String shareFileIds, String myFolderId, String shareUserId, String currentUserId) {
        String[] shareFileIdArray = shareFileIds.split(",");
        // Target file list
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getUserId, currentUserId)
                .eq(FileInfo::getFilePid, myFolderId);

        List<FileInfo> currentFileList = fileInfoMapper.selectList(queryWrapper);
        Map<String, FileInfo> currentFileMap = currentFileList.stream().collect(Collectors.toMap(
                FileInfo::getFileName, Function.identity(), (data1, data2) -> data2
        ));

        // Selected the files
        LambdaQueryWrapper<FileInfo> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(FileInfo::getUserId, shareUserId)
                .in(FileInfo::getFileId, shareFileIdArray);
        List<FileInfo> shareFileList = fileInfoMapper.selectList(queryWrapper1);

        // Rename the selected name
        LocalDateTime curDate = LocalDateTime.now();
        List<FileInfo> copyFileList = new ArrayList<>();
        for (FileInfo item : shareFileList) {
            FileInfo haveFile = currentFileMap.get(item.getFileName());
            if (haveFile == null) {
                item.setFileName(StringUtil.rename(item.getFileName()));
            }
            findAllSubFile(copyFileList, item, shareUserId, currentUserId, curDate, myFolderId);
        }
        saveBatch(copyFileList);


    }

    private void findAllSubFile(List<FileInfo> copyFileList, FileInfo fileInfo, String sourceUserId, String currentUserId, LocalDateTime curDate, String newFilePid) {
        String sourceFileId = fileInfo.getFileId();
        fileInfo.setCreateTime(curDate)
                .setLastUpdateTime(curDate)
                .setFilePid(newFilePid)
                .setUserId(currentUserId);
        String newFileId = StringUtil.getRandomString(Constants.TEN);
        fileInfo.setFileId(newFileId);
        copyFileList.add(fileInfo);
        if (FileFolderTypeEnum.FOLDER.getType().equals(fileInfo.getFolderType())) {
            LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileInfo::getFilePid, sourceFileId)
                    .eq(FileInfo::getUserId, sourceUserId);
            List<FileInfo> sourceFileList = fileInfoMapper.selectList(queryWrapper);
            for (FileInfo item : sourceFileList) {
                findAllSubFile(copyFileList, item, sourceUserId, currentUserId, curDate, newFileId);
            }
        }
    }

    private void checkFilePid(String rootFilePid, String fileId, String userId) {
        FileInfo fileInfo = fileInfoMapper.selectByFileIDAndUserID(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (Constants.ZERO_STRING.equals(fileInfo.getFilePid())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (fileInfo.getFilePath().equals(rootFilePid)) {
            return;
        }
        checkFilePid(rootFilePid, fileInfo.getFilePid(), userId);
    }


}
