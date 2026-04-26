package com.kuopan.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kuopan.Annotation.GlobalInterceptor;
import com.kuopan.Annotation.VerifyParams;
import com.kuopan.Component.RedisComponent;
import com.kuopan.Entity.FileInfo;
import com.kuopan.Entity.UserInfo;
import com.kuopan.Entity.dto.SysSettingsDto;
import com.kuopan.Service.IFileInfoService;
import com.kuopan.Service.IUserInfoService;
import com.kuopan.Util.StringUtil;
import com.kuopan.vo.PaginationResultVO;
import com.kuopan.vo.ResponseVO;
import com.kuopan.vo.UserInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.baomidou.mybatisplus.extension.toolkit.Db.page;

@RestController("adminController")
@RequestMapping("/admin")
public class AdminController extends CommonFileController {
    @Resource
    private IFileInfoService fileInfoService;

    @Resource
    private IUserInfoService userInfoService;

    @Resource
    private RedisComponent redisComponent;

    @RequestMapping("/getSysSettings")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO getSysSettings() {
        return ResponseVO.success(redisComponent.getSysSettingDto());
    }

    @RequestMapping("/saveSysSettings")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO saveSysSettings(@VerifyParams(required = true) String registerEmailTitle, @VerifyParams(required = true) String registerEmailContent, @VerifyParams(required = true) Integer userInitialSpace) {
        SysSettingsDto dto = new SysSettingsDto();
        dto.setRegisterEmailContent(registerEmailContent).setUserInitialSpace(userInitialSpace).setRegisterEmailTitle(registerEmailTitle);
        redisComponent.saveSysSettingsDto(dto);
        return ResponseVO.success(null);
    }

    @RequestMapping("/loadUserList")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO loadUserList(@RequestParam(defaultValue = "1") Integer pageNo,
                                   @RequestParam(defaultValue = "15") Integer pageSize,
                                   @RequestParam(required = false) String userNameFuzzy, @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(UserInfo::getRegTime)
                .like(!StringUtil.isEmpty(userNameFuzzy), UserInfo::getUserName, userNameFuzzy)
                .eq(status != null, UserInfo::getStatus, status);
        IPage<UserInfo> page = new Page<>(pageNo, pageSize);
        page = userInfoService.page(page, queryWrapper);
        IPage<UserInfoVO> pagevo = page.convert(entity -> {
            UserInfoVO vo = new UserInfoVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        });
        PaginationResultVO<UserInfoVO> resultVO = PaginationResultVO.convert2PaginationVO(pagevo);
        return ResponseVO.success(resultVO);
    }

    @RequestMapping("/updateUserStatus")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO updateUserStatus(@VerifyParams(required = true) String userId, @VerifyParams(required = true) Boolean status) {
        userInfoService.updateUserStatus(userId, status);
        return ResponseVO.success(null);
    }

    @RequestMapping("/updateUserSpace")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO updateUserSpace(@VerifyParams(required = true) String userId, @VerifyParams(required = true) Integer changeSpace) {
        userInfoService.changeUserSpace(userId, changeSpace);
        return ResponseVO.success(null);
    }


    @RequestMapping("/loadFileList")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO loadDataList(@RequestParam(defaultValue = "1") Integer pageNo,
                                   @RequestParam(defaultValue = "15") Integer pageSize,
                                   @RequestParam String filePid,
                                   @RequestParam(required = false) String fileNameFuzzy) {
        IPage<FileInfo> page = fileInfoService.findAllListByDescInUse(pageNo, pageSize, filePid, fileNameFuzzy);
        PaginationResultVO<FileInfo> result = PaginationResultVO.convert2PaginationVO(page);
        return ResponseVO.success(result);
    }

    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO getFolderInfo(@VerifyParams(required = true) String path) {
        return super.getFolderInfo(path, null);
    }

    @RequestMapping("/getFile/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public void getFile(HttpServletResponse response, @PathVariable("userId") String userId, @PathVariable("fileId") String fileId) {
        super.getFile(response, fileId, userId);
    }

    @RequestMapping("/ts/getVideoInfo/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public void getImage(HttpServletResponse response, @PathVariable("userId") String userId, @PathVariable("fileId") String fileId) {
        super.getFile(response, fileId, userId);
    }

    @RequestMapping("/createDownloadUrl/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO createDownloadUrl(@PathVariable("userId") String userId, @PathVariable("fileId") String fileId) {
        return super.createDownloadUrl(fileId, userId);
    }

    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public void download(HttpServletRequest request, HttpServletResponse response, @VerifyParams(required = true) @PathVariable("code") String code) throws Exception {
        super.download(request, response, code);
    }

    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO delFiles(@VerifyParams(required = true) String fileIdAndUserIds) {
        String[] fileIdAndUserIdArray = fileIdAndUserIds.split(",");
        for (String fileIdAndUserId : fileIdAndUserIdArray) {
            String[] itemArray = fileIdAndUserId.split("_");
            fileInfoService.delFileBatch(itemArray[0], itemArray[1], true);
        }
        return ResponseVO.success(null);
    }


}
