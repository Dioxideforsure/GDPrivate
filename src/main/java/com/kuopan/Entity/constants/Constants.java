package com.kuopan.Entity.constants;

import java.time.format.DateTimeFormatter;

public class Constants {
    public static final String CHECK_CODE_KEY = "check_code_key";

    public static final String CHECK_CODE_KEY_EMAIL = "check_code_key_email";

    public static final Integer LENGTH_5 = 5;

    public static final Integer ZERO = 0;

    public static final String ZERO_STRING = "0";

    public static final Integer ONE = 1;

    public static final Integer TWO = 2;

    public static final Integer THREE = 3;

    public static final Integer FOUR = 4;

    public static final Integer FIVE = 5;

    public static final Integer SIX = 6;

    public static final Integer SEVEN = 7;

    public static final Integer EIGHT = 8;

    public static final Integer NINE = 9;

    public static final Integer TEN = 10;

    public static final Integer TWENTY = 20;

    public static final Integer FIFTY = 50;

    public static final Integer ONE_FIFTY = 150;

    public static final Integer REDIS_TEMP_EXPIRE_TIME_HOUR = 1;

    public static final String REDIS_KEY_SYS_SETTINGS = "Kuopan:sysSettings";

    public static final String REDIS_KEY_PREFIX_EMAIL = "verify:email:";

    public static final String REDIS_KEY_USER_SPACE_USE = "Kuopan:user:spaceuse:";

    public static final String REDIS_KEY_USER_FILE_TEMP_SIZE = "Kuopan:user:file:temp:";

    public static final String REDIS_KEY_DOWNLOAD = "Kuopan:download:";

    public static final Long MB = 1024 * 1024l;

    public static final String SESSION_KEY = "session_key";

    public static final String SESSION_SHARE_KEY = "session_share_key";

    public static final String FILE_FOLDER_FILE = "/file/";

    public static final String TEMP_FOLDER_FILE = "/temp/";

    public static final DateTimeFormatter standard_yyyyMM = DateTimeFormatter.ofPattern("yyyy_MM");

    public static final String TS_NAME = "index.ts";

    public static final String M3U8_NAME = "index.m3u8";

    public static final String IMAGE_PNG_SUFFIX = ".png";


}
