package com.kuopan.Controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kuopan.Annotation.GlobalInterceptor;
import com.kuopan.Annotation.VerifyParams;
import com.kuopan.Entity.FileInfo;
import com.kuopan.Service.IFileInfoService;
import com.kuopan.vo.FileInfoVO;
import com.kuopan.vo.PaginationResultVO;
import com.kuopan.vo.ResponseVO;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController("recycleController")
@RequestMapping("/recycle")
public class RecycleController extends BaseController {

    @Resource
    private IFileInfoService fileInfoService;

    @RequestMapping("/loadRecycleList")
    @GlobalInterceptor
    public ResponseVO loadRecycleList(HttpSession session,
                                      @RequestParam(defaultValue = "1") Integer pageNo,
                                      @RequestParam(defaultValue = "15") Integer pageSize,
                                      @RequestParam(required = false) String fileNameFuzzy) {
        String userId = getUserInfoFromSession(session).getUserId();
        IPage<FileInfo> page = fileInfoService.findListByDesc4Recycle(pageNo, pageSize, userId);
        IPage<FileInfoVO> vopage = page.convert(entity -> {
            FileInfoVO vo = new FileInfoVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        });

        PaginationResultVO<FileInfoVO> result = PaginationResultVO.convert2PaginationVO(vopage);
        return ResponseVO.success(result);
    }

    @RequestMapping("/recoverFile")
    @GlobalInterceptor
    public ResponseVO recoverFile(HttpSession session, @VerifyParams(required = true) String fileIds) {
        String userId = getUserInfoFromSession(session).getUserId();
        fileInfoService.recoverFileBatch(userId, fileIds);
        return ResponseVO.success(null);
    }

    @RequestMapping("/delFile")
    @GlobalInterceptor
    public ResponseVO delFile(HttpSession session, @VerifyParams(required = true) String fileIds) {
        String userId = getUserInfoFromSession(session).getUserId();
        fileInfoService.delFileBatch(userId, fileIds, false);
        return ResponseVO.success(null);
    }
}
