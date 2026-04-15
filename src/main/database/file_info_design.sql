create table file_info
(
    file_id          varchar(10)  not null comment 'The file id.',
    user_id          varchar(36)  not null comment 'The ID of the file belongs to the user.',
    file_md5         varchar(32)  null comment 'The MD5 number of the file.',
    file_pid         varchar(10)  null comment 'The parent file id.',
    file_size        bigint       null comment 'The size of file, byte as the unit.',
    file_name        varchar(200) null comment 'The name of the file.',
    file_cover       varchar(100) null comment 'The cover of the file, e.g. photo and video',
    file_path        varchar(100) null comment 'The path of the file.',
    create_time      datetime     null comment 'The first create time',
    last_update_time datetime     null comment 'The latest upload time.',
    folder_type      tinyint(1)   null comment 'The type of file, 0 is file, 1 is catalog.',
    file_category    tinyint(1)   null comment 'The category of file. 1 is video, 2 is audio, 3 is photo, 4 is
document, 5 is other.',
    file_type        tinyint(1)   null comment 'The type of file, 1 is video, 2 is audio, 3 is photo, 4 is pdf, 5 is doc, 6 is excel,
7 is txt, 8 is code, 9 is zip, 10 is other.',
    status           tinyint(1)   null comment 'The current file status, 0 is tranforming ,1 is failed, 2 is successed',
    recovery_time    datetime     null comment 'The time in recycle bin',
    del_flag         tinyint(1)   null comment 'The mark of delete, 0 is deletion, 1 is in recycle bin, 2 is normal.',
    primary key (file_id, user_id)
)
    comment 'The file detailed information.';

create index idx_create_time
    on file_info (create_time)
    comment 'The create time index';

create index idx_del_flag
    on file_info (del_flag)
    comment 'The delete flag index';

create index idx_file_pid
    on file_info (file_pid)
    comment 'The file pid index';

create index idx_md5
    on file_info (file_md5)
    comment 'The MD5 index';

create index idx_recovery_time
    on file_info (recovery_time)
    comment 'The index of deletion time.';

create index idx_status
    on file_info (status);

create index idx_user_id
    on file_info (user_id)
    comment 'The user id index';


