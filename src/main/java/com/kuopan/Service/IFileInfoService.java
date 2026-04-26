package com.kuopan.Service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kuopan.Entity.FileInfo;
import com.kuopan.Entity.dto.SessionWebUserDto;
import com.kuopan.Entity.dto.UploadResultDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * The file detailed information. 服务类
 * </p>
 *
 * @author Dioxide
 * @since 2026-04-14
 */
public interface IFileInfoService extends IService<FileInfo> {

    IPage<FileInfo> findListByDescInUse(Integer pageNo, Integer pageSize, String userId, String category, String filePid, String fileNameFuzzy);

    IPage<FileInfo> findAllListByDescInUse(Integer pageNo, Integer pageSize,String filePid, String fileNameFuzzy);

    UploadResultDto uploadFile(SessionWebUserDto userDto, String fileId, MultipartFile file,
                               String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks);

    FileInfo getFileInfoByUserIdAndFileId(String userId, String fileId);

    FileInfo newFolder(String filePid, String userId, String folderName);

    FileInfo rename(String fileId, String userId, String fileName);

    List<FileInfo> loadAllFolder(String userId, String filePid, String currentFileIds);

    void changeFileFolder(String fileIds, String filePid, String userId);

    void removeFile2RecycleBatch(String userId, String fileIds);

    IPage<FileInfo> findListByDesc4Recycle(Integer pageNo, Integer pageSize, String userId);

    void recoverFileBatch(String userId, String fileIds);

    void delFileBatch(String userId, String fileIds, Boolean adminOp);

    void checkRootFilePid(String filePid, String userId, String fileId);

    IPage<FileInfo> findFileInShareFolder(String filePid, String fileId, String userId, Integer pageNo, Integer pageSize);

    void saveShare(String shareRootFilePid, String shareFileIds, String myFolderId, String shareUserId, String currentUserId);
}
