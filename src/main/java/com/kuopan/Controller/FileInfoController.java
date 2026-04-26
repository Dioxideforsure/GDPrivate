package com.kuopan.Controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kuopan.Annotation.GlobalInterceptor;
import com.kuopan.Annotation.VerifyParams;
import com.kuopan.Entity.FileInfo;
import com.kuopan.Entity.dto.SessionWebUserDto;
import com.kuopan.Entity.dto.UploadResultDto;
import com.kuopan.Service.IFileInfoService;
import com.kuopan.vo.FileInfoVO;
import com.kuopan.vo.PaginationResultVO;
import com.kuopan.vo.ResponseVO;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * The file detailed information. 前端控制器
 * </p>
 *
 * @author Dioxide
 * @since 2026-04-14
 */


@RestController("fileInfoController")
@RequestMapping("/file")
public class FileInfoController extends CommonFileController {

    @Resource
    private IFileInfoService fileInfoService;

    // Load the files
    @RequestMapping("/loadDataList")
    @GlobalInterceptor
    public ResponseVO loadDataList(HttpSession session,
                                   @RequestParam(defaultValue = "1") Integer pageNo,
                                   @RequestParam(defaultValue = "15") Integer pageSize,
                                   @RequestParam(required = false) String filePid,
                                   @RequestParam(required = false) String fileNameFuzzy,
                                   String category) {
        String userId = getUserInfoFromSession(session).getUserId();
        IPage<FileInfo> page = fileInfoService.findListByDescInUse(pageNo, pageSize, userId, category, filePid, fileNameFuzzy);
        IPage<FileInfoVO> vopage = page.convert(entity -> {
            FileInfoVO vo = new FileInfoVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        });

        PaginationResultVO<FileInfoVO> result = PaginationResultVO.convert2PaginationVO(vopage);
        return ResponseVO.success(result);
    }

    // Upload the file
    @RequestMapping("/uploadFile")
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

    // Get the image
    @RequestMapping("/getImage/{imageFolder}/{imageName}")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public void getImage(HttpServletResponse response, @PathVariable("imageFolder") String imageFolder, @PathVariable("imageName") String imageName) {
        super.getImage(response, imageFolder, imageName);
    }

    // Get the sharding video
    @RequestMapping("/ts/getVideoInfo/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public void getImage(HttpServletResponse response, HttpSession session, @PathVariable("fileId") String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webUserDto.getUserId());
    }

    // Get the other file
    @RequestMapping("/getFile/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public void getFile(HttpServletResponse response, HttpSession session, @PathVariable("fileId") String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webUserDto.getUserId());
    }

    // New Folder
    @RequestMapping("/newFolder")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO newFolder(HttpSession session,
                                @VerifyParams(required = true) String filePid,
                                @VerifyParams(required = true) String fileName
    ) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.newFolder(filePid, webUserDto.getUserId(), fileName);
        FileInfoVO fileInfoVO = new FileInfoVO();
        BeanUtils.copyProperties(fileInfo, fileInfoVO);
        return ResponseVO.success(fileInfoVO);
    }

    // Get the folder info
    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO getFolderInfo(HttpSession session,
                                    @VerifyParams(required = true) String path) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);

        return super.getFolderInfo(path, webUserDto.getUserId());
    }

    // Rename
    @RequestMapping("/rename")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO rename(HttpSession session,
                             @VerifyParams(required = true) String fileId,
                             @VerifyParams(required = true) String fileName
    ) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.rename(fileId, webUserDto.getUserId(), fileName);
        FileInfoVO fileInfoVO = new FileInfoVO();
        BeanUtils.copyProperties(fileInfo, fileInfoVO);
        return ResponseVO.success(fileInfoVO);
    }

    // Get all the folder
    @RequestMapping("/loadAllFolder")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadAllFolder(HttpSession session,
                                    @VerifyParams(required = true) String filePid,
                                    String currentFileIds
    ) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        List<FileInfo> fileInfoList = fileInfoService.loadAllFolder(webUserDto.getUserId(), filePid, currentFileIds);
        List<FileInfoVO> fileInfoVOList = fileInfoList.stream().map(entity -> {
            FileInfoVO fileInfoVO = new FileInfoVO();
            BeanUtils.copyProperties(entity, fileInfoVO);
            return fileInfoVO;
        }).collect(Collectors.toList());
        return ResponseVO.success(fileInfoVOList);
    }

    // Move the files to a new folder
    @RequestMapping("/changeFileFolder")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO changeFileFolder(HttpSession session,
                                       @VerifyParams(required = true) String fileIds,
                                       @VerifyParams(required = true) String filePid
    ) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.changeFileFolder(fileIds, filePid, webUserDto.getUserId());
        return ResponseVO.success(null);
    }

    // Download
    @RequestMapping("/createDownloadUrl/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO createDownloadUrl(HttpSession session,
                                        @VerifyParams(required = true) @PathVariable("fileId") String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        return super.createDownloadUrl(fileId, webUserDto.getUserId());
    }

    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public void download(HttpServletRequest request, HttpServletResponse response, @VerifyParams(required = true) @PathVariable("code") String code) throws Exception {
        super.download(request, response, code);
    }

    // Delete files
    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO delFiles(HttpSession session,
                               @VerifyParams(required = true) String fileIds) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.removeFile2RecycleBatch(webUserDto.getUserId(), fileIds);
        return ResponseVO.success(null);
    }


}
