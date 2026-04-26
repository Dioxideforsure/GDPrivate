package com.kuopan.Entity.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DownloadFileDto {
    private String downloadCode;
    private String fileName;
    private String filePath;
}
