package com.kuopan.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * <p>
 * To put in User Infomation
 * </p>
 *
 * @author Kuo
 * @since 2026-01-13
 */
@Getter
@Setter
@TableName("user_info")
@Data
public class UserInfo implements IEntity {

    private static final long serialVersionUID = 1L;

    /**
     * To put UserID in GUID string
     */
    @TableId("user_id")
    private String userId;

    /**
     * To put the nickname of users
     */
    @TableField("user_name")
    private String userName;

    /**
     * To put in the emails from user
     */
    @TableField("email")
    private String email;

    /**
     * To put in password in MD5
     */
    @TableField("password")
    private String password;

    /**
     * Registry time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime regTime;

    /**
     * The status of users.0 is banned, 1 is enabled
     */
    @TableField("status")
    private Boolean status;

    /**
     * The occupied space, use byte.
     */
    @TableField("occu_space")
    private Long occuSpace;

    /**
     * The total space of user
     */
    @TableField("total_space")
    private Long totalSpace;

    /**
     * Apply the role for the user.0 is admin, 1 is teacher, 2 is student,3 is guest.
     */
    @TableField("role")
    private Byte role;
}
