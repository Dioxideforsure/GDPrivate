package com.kuopan.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileInfoVO {

    private String fileId;
    private String filePid;
    private Long fileSize;
    private String fileName;
    private String fileCover;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recoveryTime;
    private Boolean folderType;
    private Integer fileCategory;
    private Integer fileType;
    private Integer status;
}
