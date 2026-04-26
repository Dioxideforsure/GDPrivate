package com.kuopan.Controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kuopan.Annotation.GlobalInterceptor;
import com.kuopan.Annotation.VerifyParams;
import com.kuopan.Entity.FileInfo;
import com.kuopan.Entity.FileShare;
import com.kuopan.Entity.UserInfo;
import com.kuopan.Entity.constants.Constants;
import com.kuopan.Entity.dto.SessionShareDto;
import com.kuopan.Entity.dto.SessionWebUserDto;
import com.kuopan.Entity.enums.FileDelFlag;
import com.kuopan.Entity.enums.ResponseCodeEnum;
import com.kuopan.Exception.BusinessException;
import com.kuopan.Service.IFileInfoService;
import com.kuopan.Service.IFileShareService;
import com.kuopan.Service.IUserInfoService;
import com.kuopan.Util.StringUtil;
import com.kuopan.vo.FileInfoVO;
import com.kuopan.vo.PaginationResultVO;
import com.kuopan.vo.ResponseVO;
import com.kuopan.vo.ShareInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;

@RestController("webShareController")
@RequestMapping("/showShare")
public class WebShareController extends CommonFileController {

    @Resource
    private IFileShareService fileShareService;

    @Resource
    private IFileInfoService fileInfoService;

    @Resource
    private IUserInfoService userInfoService;

    @RequestMapping("/getShareLoginInfo")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public ResponseVO getShareLoginInfo(HttpSession session,
                                        @VerifyParams(required = true) String shareId) {
        SessionShareDto sessionShareDto = getShareFromSession(session, shareId);
        if (sessionShareDto == null) {
            return ResponseVO.success(null);
        }
        ShareInfoVO shareInfoVO = getShareInfoCommon(shareId);
        // Judge if the current user shares.
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        shareInfoVO.setCurrentUser(false);
        if (userDto != null && userDto.getUserId().equals(sessionShareDto.getShareUserId())) {
            shareInfoVO.setCurrentUser(true);
        }
        return ResponseVO.success(shareInfoVO);
    }


    @RequestMapping("/getShareInfo")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public ResponseVO getShareInfo(@VerifyParams(required = true) String shareId) {
        return ResponseVO.success(getShareInfoCommon(shareId));
    }

    @RequestMapping("/checkShareCode")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public ResponseVO checkShareCode(HttpSession session,
                                     @VerifyParams(required = true) String shareId,
                                     @VerifyParams(required = true) String code) {
        SessionShareDto sessionShareDto = fileShareService.checkShareCode(shareId, code);
        session.setAttribute(Constants.SESSION_SHARE_KEY + shareId, sessionShareDto);
        return ResponseVO.success(null);
    }

    @RequestMapping("/loadFileList")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public ResponseVO loadFileList(HttpSession session,
                                   @VerifyParams(required = true) String shareId,
                                   String filePid,
                                   @RequestParam(defaultValue = "1") Integer pageNo,
                                   @RequestParam(defaultValue = "15") Integer pageSize) {
        SessionShareDto shareDto = checkShare(session, shareId);
        IPage<FileInfo> page;
        if (!StringUtil.isEmpty(filePid) && !Constants.ZERO_STRING.equals(filePid)) {
            fileInfoService.checkRootFilePid(shareDto.getFileId(), shareDto.getShareUserId(), filePid);
            page = fileInfoService.findFileInShareFolder(filePid, null, shareDto.getShareUserId(), pageNo, pageSize);
        } else {
            page = fileInfoService.findFileInShareFolder(null, shareDto.getFileId(), shareDto.getShareUserId(), pageNo, pageSize);
        }
        IPage<FileInfoVO> pagevo = page.convert(entity -> {
            FileInfoVO vo = new FileInfoVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        });
        PaginationResultVO<FileInfoVO> resultVO = PaginationResultVO.convert2PaginationVO(pagevo);
        return ResponseVO.success(resultVO);
    }

    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public ResponseVO getFolderInfo(HttpSession session,
                                    @VerifyParams(required = true) String shareId,
                                    @VerifyParams(required = true) String path) {
        SessionShareDto shareDto = checkShare(session, shareId);
        return super.getFolderInfo(path, shareDto.getShareUserId());
    }

    @RequestMapping("/getFile/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public void getFile(HttpSession session, HttpServletResponse response,
                        @PathVariable("shareId") String shareId,
                        @PathVariable("fileId") String fileId) {
        SessionShareDto shareDto = checkShare(session, shareId);
        super.getFile(response, fileId, shareDto.getShareUserId());
    }

    @RequestMapping("/ts/getVideoInfo/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public void getImage(HttpSession session, HttpServletResponse response,
                         @PathVariable("shareId") String shareId,
                         @PathVariable("fileId") String fileId) {
        SessionShareDto shareDto = checkShare(session, shareId);
        super.getFile(response, fileId, shareDto.getShareUserId());
    }

    @RequestMapping("/createDownloadUrl/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public ResponseVO createDownloadUrl(HttpSession session, @PathVariable("shareId") String shareId, @PathVariable("fileId") String fileId) {
        SessionShareDto shareDto = checkShare(session, shareId);
        return super.createDownloadUrl(fileId, shareDto.getShareUserId());
    }

    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public void download(HttpServletRequest request, HttpServletResponse response, @VerifyParams(required = true) @PathVariable("code") String code) throws Exception {
        super.download(request, response, code);
    }

    @RequestMapping("/saveShare")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO saveShare(HttpSession session,
                          @VerifyParams(required = true) String shareId,
                          @VerifyParams(required = true) String shareFileIds,
                          @VerifyParams(required = true) String myFolderId) throws Exception {
        SessionShareDto shareDto = checkShare(session, shareId);
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        if (shareDto.getShareUserId().equals(webUserDto.getUserId())) {
            throw new BusinessException("自己分享的文件无法保存到自己的网盘");
        }
        fileInfoService.saveShare(shareDto.getFileId(), shareFileIds, myFolderId, shareDto.getShareUserId(), webUserDto.getUserId());
        return ResponseVO.success(null);
    }

    private ShareInfoVO getShareInfoCommon(String shareId) {
        FileShare fileShare = fileShareService.getById(shareId);
        if (fileShare == null || (fileShare.getExpireTime() != null && LocalDateTime.now().isAfter(fileShare.getExpireTime()))) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        ShareInfoVO shareInfoVO = new ShareInfoVO();
        BeanUtils.copyProperties(fileShare, shareInfoVO);
        FileInfo fileInfo = fileInfoService.getFileInfoByUserIdAndFileId(fileShare.getUserId(), fileShare.getFileId());
        if (fileInfo == null || !FileDelFlag.USING.getFlag().equals(fileInfo.getDelFlag())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        UserInfo userInfo = userInfoService.selectByUserId(fileShare.getUserId());
        shareInfoVO.setUserName(userInfo.getUserName()).setUserId(userInfo.getUserId()).setFileName(fileInfo.getFileName());
        return shareInfoVO;
    }

    private SessionShareDto checkShare(HttpSession session, String shareId) {
        SessionShareDto shareDto = getShareFromSession(session, shareId);
        if (shareDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_903);
        }
        if (shareDto.getExpireTime() != null && LocalDateTime.now().isAfter(shareDto.getExpireTime())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        return shareDto;
    }
}
