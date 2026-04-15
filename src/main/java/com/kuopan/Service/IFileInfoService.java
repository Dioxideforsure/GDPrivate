package com.kuopan.Service;


import com.baomidou.mybatisplus.core.metadata.IPage;import com.baomidou.mybatisplus.extension.service.IService;import com.kuopan.Entity.FileInfo;
import com.kuopan.Entity.dto.SessionWebUserDto;
import com.kuopan.Entity.dto.UploadResultDto;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * The file detailed information. 服务类
 * </p>
 *
 * @author Dioxide
 * @since 2026-04-14
 */
public interface IFileInfoService extends IService<FileInfo> {

     IPage<FileInfo> findListByDescInUse(Integer pageNo, Integer pageSize, String userId, String category);

     UploadResultDto uploadFile(SessionWebUserDto userDto, String fileId, MultipartFile file,
                                String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks);
}
