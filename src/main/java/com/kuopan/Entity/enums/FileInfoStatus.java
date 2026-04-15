package com.kuopan.Entity.enums;

import lombok.Getter;

@Getter
public enum FileInfoStatus {
    TRANSFORM(0, "转码中"),
    TRANSFORM_FAIL(1, "转码失败"),
    USING(2, "使用中");

    private Integer status;
    private String desc;

    FileInfoStatus(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }
}
