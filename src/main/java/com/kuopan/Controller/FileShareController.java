package com.kuopan.Controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kuopan.Annotation.GlobalInterceptor;
import com.kuopan.Annotation.VerifyParams;
import com.kuopan.Entity.FileShare;import com.kuopan.Service.IFileShareService;
import com.kuopan.vo.PaginationResultVO;
import com.kuopan.vo.ResponseVO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * <p>
 * The table for describing the share actions 前端控制器
 * </p>
 *
 * @author Dioxide
 * @since 2026-04-20
 */
@RestController("fileShareController")
@RequestMapping("/share")
public class FileShareController extends BaseController{

    @Resource
    private IFileShareService fileShareService;

    @RequestMapping("/loadList")
    @GlobalInterceptor
    public ResponseVO loadDataList(HttpSession session, @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,@RequestParam(value = "pageSize", defaultValue = "15") Integer pageSize) {
        String userId = getUserInfoFromSession(session).getUserId();
        IPage<FileShare> page = fileShareService.findListByDescInShareTime(pageNo, pageSize, userId);
        PaginationResultVO<FileShare> result = PaginationResultVO.convert2PaginationVO(page);
        return ResponseVO.success(result);
    }

    @RequestMapping("/shareFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO shareFile(HttpSession session,
                                @VerifyParams(required = true) String fileId,
                                @VerifyParams(required = true) Integer validType,
                                String code) {
        String userId = getUserInfoFromSession(session).getUserId();
        FileShare share = new FileShare();
        share.setCode(code)
                .setUserId(userId)
                .setValidType(validType)
                .setFileId(fileId)
                .setShowCount(0);
        fileShareService.saveShare(share);
        return ResponseVO.success(share);
    }

    @RequestMapping("/cancelShare")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO cancelShare(HttpSession session,
                                @VerifyParams(required = true) String shareIds) {
        String userId = getUserInfoFromSession(session).getUserId();
        fileShareService.deleteFileShareBatch(shareIds.split(","), userId);
        return ResponseVO.success(null);
    }

}
