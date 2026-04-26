package com.kuopan.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kuopan.Entity.FileInfo;
import com.kuopan.Entity.enums.FileDelFlag;
import com.kuopan.Service.IFileInfoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FileDeleteTask {
    @Resource
    private IFileInfoService fileInfoService;

    @Scheduled(fixedDelay = 10000 * 60 * 3)
    public void execute() {
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getDelFlag, FileDelFlag.RECYCLE.getFlag())
                .lt(FileInfo::getRecoveryTime, LocalDateTime.now().minusDays(10));
        List<FileInfo> fileInfoList = fileInfoService.list(queryWrapper);
        Map<String, List<FileInfo>> fileInfoMap = fileInfoList.stream()
                .collect(Collectors.groupingBy(FileInfo::getUserId));
        for (Map.Entry<String, List<FileInfo>> entry : fileInfoMap.entrySet()) {
            List<String> fileIds = entry.getValue().stream().map(
                    p -> p.getFileId()
            ).collect(Collectors.toList());
            fileInfoService.delFileBatch(entry.getKey(), String.join(",", fileIds), false);

        }
    }
}
