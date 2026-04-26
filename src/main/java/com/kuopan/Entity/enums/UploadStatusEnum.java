package com.kuopan.Entity.enums;

import lombok.Getter;

@Getter
public enum UploadStatusEnum {
    FLASH_UPLOAD("upload_seconds", "秒传"),
    UPLOADING("uploading", "上传中"),
    UPLOADED("upload_finish", "上传完成");

    private String code;
    private String desc;

    UploadStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
