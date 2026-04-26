package com.kuopan.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kuopan.Component.RedisComponent;
import com.kuopan.Config.AppConfig;
import com.kuopan.Entity.FileInfo;
import com.kuopan.Entity.constants.Constants;
import com.kuopan.Entity.dto.DownloadFileDto;
import com.kuopan.Entity.enums.FileCategoryEnum;
import com.kuopan.Entity.enums.FileFolderTypeEnum;
import com.kuopan.Entity.enums.ResponseCodeEnum;
import com.kuopan.Exception.BusinessException;
import com.kuopan.Service.IFileInfoService;
import com.kuopan.Util.StringUtil;
import com.kuopan.vo.FolderVO;
import com.kuopan.vo.ResponseVO;
import org.springframework.beans.BeanUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;


public class CommonFileController extends BaseController {

    @Resource
    private AppConfig appConfig;

    @Resource
    private IFileInfoService fileInfoService;

    @Resource
    private RedisComponent redisComponent;

    protected void getImage(HttpServletResponse response, String imageFolder, String imageName) {
        if (StringUtil.isEmpty(imageFolder) || StringUtil.isEmpty(imageName) || !StringUtil.pathIsOk(imageFolder) || !StringUtil.pathIsOk(imageName)) {
            return;
        }
        String imageSuffix = StringUtil.getFileNameSuffix(imageName);
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + imageFolder + "/" + imageName;
        imageSuffix = imageSuffix.replace(".", "");
        String contentType = "image/" + imageSuffix;
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "max-age=2592000");
        readFile(response, filePath);
    }

    // Sharding
    protected void getFile(HttpServletResponse response, String fileId, String userId) {
        String filePath = null;
        if (fileId.endsWith(".ts")) {
            String[] tsArray = fileId.split("_");
            String fileRealId = tsArray[0];
            FileInfo fileInfo = fileInfoService.getFileInfoByUserIdAndFileId(userId, fileRealId);
            if (fileInfo == null) {
                return;
            }
            String fileName = fileInfo.getFilePath();
            fileName = StringUtil.getFileNameNoSuffix(fileName) + "/" + fileId;
            filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileName;
        } else {
            FileInfo fileInfo = fileInfoService.getFileInfoByUserIdAndFileId(userId, fileId);
            if (fileInfo == null) {
                return;
            }
            if (FileCategoryEnum.VIDEO.getCategory().equals(fileInfo.getFileCategory())) {
                String fileNameNoSuffix = StringUtil.getFileNameNoSuffix(fileInfo.getFilePath());
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileNameNoSuffix + "/" + Constants.M3U8_NAME;
            } else {
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileInfo.getFilePath();
            }

            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }
        }
        readFile(response, filePath);
    }

    // Get folder info
    protected ResponseVO getFolderInfo(String path, String userId) {
        String[] pathArray = path.split("/");
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        String orderBy = "order by field(file_id, \"" + String.join("\",\"", pathArray) + "\")";
        queryWrapper.in(FileInfo::getFileId, pathArray)
                .eq(!StringUtil.isEmpty(userId), FileInfo::getUserId, userId)
                .eq(FileInfo::getFolderType, FileFolderTypeEnum.FOLDER.getType())
                .last(orderBy);
        List<FileInfo> fileInfoList = fileInfoService.list(queryWrapper);
        List<FolderVO> FolderVOList = fileInfoList.stream().map(entity -> {
            FolderVO vo = new FolderVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).collect(Collectors.toList());
        return ResponseVO.success(FolderVOList);
    }

    // Create download url
    protected ResponseVO createDownloadUrl(String fileId, String userId) {
        FileInfo fileInfo = fileInfoService.getFileInfoByUserIdAndFileId(userId, fileId);
        if (fileInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (FileFolderTypeEnum.FOLDER.getType().equals(fileInfo.getFolderType())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String code = StringUtil.getRandomString(Constants.FIFTY);

        DownloadFileDto downloadFileDto = new DownloadFileDto();
        downloadFileDto.setDownloadCode(code)
                .setFilePath(fileInfo.getFilePath())
                .setFileName(fileInfo.getFileName());

        redisComponent.saveDownloadCode(code, downloadFileDto);
        return ResponseVO.success(code);
    }

    // Download
    protected void download(HttpServletRequest request, HttpServletResponse response, String code) throws Exception {
        DownloadFileDto downloadFileDto = redisComponent.getDownloadCode(code);
        if (downloadFileDto == null) {
            return;
        }
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + downloadFileDto.getFilePath();
        String fileName = downloadFileDto.getFileName();
        response.setContentType("application/x-msdownload; charset=UTF-8");
        if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0) {
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } else {
            fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
        }
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        readFile(response, filePath);
    }
}
