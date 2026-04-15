package com.kuopan.Entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * The file detailed information.
 * </p>
 *
 * @author Dioxide
 * @since 2026-04-14
 */
@Getter
@Setter
@ToString
@TableName("file_info")
@Accessors(chain = true)
public class FileInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The file id.
     */
    @TableId("file_id")
    private String fileId;

    /**
     * The ID of the file belongs to the user.
     */
    @TableId("user_id")
    private String userId;

    /**
     * The MD5 number of the file.
     */
    private String fileMd5;

    /**
     * The parent file id.
     */
    private String filePid;

    /**
     * The size of file, byte as the unit.
     */
    private Long fileSize;

    /**
     * The name of the file.
     */
    private String fileName;

    /**
     * The cover of the file, e.g. photo and video
     */
    private String fileCover;

    /**
     * The path of the file.
     */
    private String filePath;

    /**
     * The first create time
     */
    private LocalDateTime createTime;

    /**
     * The latest upload time.
     */
    private LocalDateTime lastUpdateTime;

    /**
     * The type of file, 0 is file, 1 is catalog.
     */
    private Boolean folderType;

    /**
     * The category of file. 1 is video, 2 is audio, 3 is photo, 4 is
document, 5 is other.
     */
    private Integer fileCategory;

    /**
     * The type of file, 1 is video, 2 is audio, 3 is photo, 4 is pdf, 5 is doc, 6 is excel,
7 is txt, 8 is code, 9 is zip, 10 is other.
     */
    private Integer fileType;

    /**
     * The current file status, 0 is tranforming ,1 is failed, 2 is successed
     */
    private Integer status;

    /**
     * The time in recycle bin
     */
    private LocalDateTime recoveryTime;

    /**
     * The mark of delete, 0 is deletion, 1 is in recycle bin, 2 is normal.
     */
    private Integer delFlag;
}
