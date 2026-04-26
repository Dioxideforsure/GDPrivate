create table file_share
(
    share_id    varchar(20)   not null comment 'The share action id'
        primary key,
    file_id     varchar(10)   null comment 'The shared file id.',
    user_id     varchar(36)   null comment 'The user id who shares this file.',
    valid_type  tinyint(1)    null comment 'The type of the valid day.0 is one day, 1 is seven days,
2 is thirty days, 3 is forever ',
    expire_time datetime      null comment 'The expired time',
    share_time  datetime      null comment 'The share time when this created',
    code        varchar(5)    null comment 'The password of this share. ',
    show_count  int default 0 null comment 'The count of the share link is shown'
)
    comment 'The table for describing the share actions';


