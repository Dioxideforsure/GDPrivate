package com.kuopan.Entity.query;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileInfoQuery {
    private Integer pageNo;
    private Integer pageSize;
    private String fileNameFuzzy;
    private String category;
    private String userId;
    private Integer filePid;
}
