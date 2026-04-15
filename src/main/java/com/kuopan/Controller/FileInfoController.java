package com.kuopan.Controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kuopan.Annotation.GlobalInterceptor;
import com.kuopan.Annotation.VerifyParams;
import com.kuopan.Entity.FileInfo;
import com.kuopan.Entity.dto.SessionWebUserDto;
import com.kuopan.Entity.dto.UploadResultDto;
import com.kuopan.Service.IFileInfoService;
import com.kuopan.vo.FileInfoVO;
import com.kuopan.vo.ResponseVO;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;import javax.servlet.http.HttpSession;

/**
 * <p>
 * The file detailed information. 前端控制器
 * </p>
 *
 * @author Dioxide
 * @since 2026-04-14
 */


@RestController("fileInfoController")
@RequestMapping("file")
public class FileInfoController extends BaseController {

    @Resource
    private IFileInfoService fileInfoService;

    // Load the files
    @RequestMapping("loadDataList")
    @GlobalInterceptor
    public ResponseVO loadDataList(HttpSession session,
                                   @RequestParam(defaultValue = "1") Integer pageNo,
                                   @RequestParam(defaultValue = "15") Integer pageSize,
                                   String category) {
        String userId = getUserInfoFromSession(session).getUserId();
        IPage<FileInfo> page = fileInfoService.findListByDescInUse(pageNo, pageSize, userId, category);
        IPage<FileInfoVO> vopage = page.convert(entity -> {
            FileInfoVO vo = new FileInfoVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        });
        return ResponseVO.success(vopage);
    }

    // Upload the file
    @RequestMapping("uploadFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO uploadFile(HttpSession session,
                                 String fileId,
                                 MultipartFile file,
                                 @VerifyParams(required = true) String fileName,
                                 @VerifyParams(required = true) String filePid,
                                 @VerifyParams(required = true) String fileMd5,
                                 @VerifyParams(required = true) Integer chunkIndex,
                                 @VerifyParams(required = true) Integer chunks) {
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        UploadResultDto resultDto = fileInfoService.uploadFile(userDto, fileId, file, fileName, filePid, fileMd5, chunkIndex, chunks);
        return ResponseVO.success(resultDto);
    }
}
