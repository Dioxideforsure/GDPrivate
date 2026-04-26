package com.kuopan.Service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kuopan.Entity.FileInfo;
import com.kuopan.Entity.FileShare;
import com.kuopan.Entity.dto.SessionShareDto;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * The table for describing the share actions 服务类
 * </p>
 *
 * @author Dioxide
 * @since 2026-04-20
 */
public interface IFileShareService extends IService<FileShare> {
    IPage<FileShare> findListByDescInShareTime(@RequestParam(defaultValue = "1") Integer pageNo, @RequestParam(defaultValue = "15") Integer pageSize, String userId);

    void saveShare(FileShare fileShare);

    void deleteFileShareBatch(String[] shareIdArray, String userId);

    SessionShareDto checkShareCode(String shareId, String code);

}
