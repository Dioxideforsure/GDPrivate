package com.kuopan.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileInfoVO {

    private String fileID;
    private String filePid;
    private Long fileSize;
    private String fileName;
    private String fileCover;
    private LocalDateTime lastUpdateTime;
    private Boolean folderType;
    private Integer fileCategory;
    private Integer fileType;
    private Integer status;
}
