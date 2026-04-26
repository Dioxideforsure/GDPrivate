package com.kuopan.Entity.query;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserInfoQuery {
    private Integer pageNo;
    private Integer pageSize;
    private String nickNameFuzzy;
    private Boolean status;
    private String email;

    private String orderBy;
}
