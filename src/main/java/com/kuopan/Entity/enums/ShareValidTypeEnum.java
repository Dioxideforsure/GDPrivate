package com.kuopan.Entity.enums;


import lombok.Getter;

@Getter
public enum ShareValidTypeEnum {
    DAY_ONE(0, 1, "一天"),
    DAY_SEVEN(1, 7, "七天"),
    DAY_THIRTY(2, 30, "三十天"),
    FOREVER(3, -1, "永久有效");

    private Integer type;
    private Integer days;
    private String desc;

    ShareValidTypeEnum(Integer type, Integer days, String desc) {
        this.type = type;
        this.days = days;
        this.desc = desc;
    }

    public static ShareValidTypeEnum getByType(Integer type) {
        for (ShareValidTypeEnum typeEnum : ShareValidTypeEnum.values()) {
            if (typeEnum.getType().equals(type)) {
                return typeEnum;
            }
        }
        return null;
    }
}
