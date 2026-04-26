package com.kuopan.Entity.enums;

import lombok.Getter;

@Getter
public enum FileFolderTypeEnum {
    FILE(false, "文件"),
    FOLDER(true, "目录");

    private Boolean type;
    private String desc;

    FileFolderTypeEnum(Boolean type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
