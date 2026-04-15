package com.kuopan.Entity.enums;

import lombok.Getter;

@Getter
public enum FileCategoryEnum {
    VIDEO(1, "video", "视频"),
    MUSIC(2, "music", "音频"),
    IMAGE(3, "image", "图片"),
    DOC(4, "doc", "文档"),
    OTHERS(5, "others", "其他");

    private Integer category;
    private String code;
    private String desc;

    FileCategoryEnum(Integer category, String code, String desc) {
        this.category = category;
        this.code = code;
        this.desc = desc;
    }

    public static FileCategoryEnum getByCode(String code) {
        for (int i = 0; i < FileCategoryEnum.values().length; i++) {
            if (FileCategoryEnum.values()[i].getCode().equals(code)) {
                return FileCategoryEnum.values()[i];
            }
        }
        return null;
    }

}
