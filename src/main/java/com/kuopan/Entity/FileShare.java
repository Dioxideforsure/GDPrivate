package com.kuopan.Entity;

import com.baomidou.mybatisplus.annotation.TableField;import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * <p>
 * The table for describing the share actions
 * </p>
 *
 * @author Dioxide
 * @since 2026-04-20
 */
@Getter
@Setter
@ToString
@TableName("file_share")
@Accessors(chain = true)
public class FileShare implements IEntity {

    private static final long serialVersionUID = 1L;

    /**
     * The share action id
     */
    @TableId("share_id")
    private String shareId;

    /**
     * The shared file id.
     */
    private String fileId;

    /**
     * The user id who shares this file.
     */
    private String userId;

    /**
     * The type of the valid day.0 is one day, 1 is seven days,
2 is thirty days, 3 is forever 
     */
    private Integer validType;

    /**
     * The expired time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    /**
     * The share time when this created
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shareTime;

    /**
     * The password of this share. 
     */
    private String code;

    /**
     * The count of the share link is shown
     */
    private Integer showCount;


    /**
     * The name of the share file, which doesn't exist, only use when need name.
     */
    @TableField(exist = false)
    private String fileName;

    /**
     * The folder type of the share file, which doesn't exist, only use when need name.
     */
    @TableField(exist = false)
    private Integer folderType;

    /**
     * The file category of the share file, which doesn't exist, only use when need name.
     */
    @TableField(exist = false)
    private Integer fileCategory;

    /**
     * The file cover of the share file, which doesn't exist, only use when need name.
     */
    @TableField(exist = false)
    private String fileCover;

    /**
     * The file type of the share file, which doesn't exist, only use when need name.
     */
    @TableField(exist = false)
    private Integer fileType;
}
