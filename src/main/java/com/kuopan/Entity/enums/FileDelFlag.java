package com.kuopan.Entity.enums;

import lombok.Getter;

@Getter
public enum FileDelFlag {
    DEL(0, "删除"),
    RECYCLE(1, "回收站"),
    USING(2, "使用中");

    private Integer flag;
    private String desc;

    FileDelFlag(Integer flag, String desc) {
        this.flag = flag;
        this.desc = desc;
    }
}
