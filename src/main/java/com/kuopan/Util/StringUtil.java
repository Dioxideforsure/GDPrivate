package com.kuopan.Util;

import com.kuopan.Entity.constants.Constants;
import org.apache.commons.lang3.RandomStringUtils;

public class StringUtil {

    /**
     * Generate random string
     *
     * @param count
     * @return
     *
     */
    public static final String getRandomString(Integer count) {
        return RandomStringUtils.random(count, true, true);
    }

    /**
     * Generate Random Number
     *
     * @param count
     * @return
     * */
    public static final String getRandomNumber(Integer count) {
        return RandomStringUtils.random(count, false, true);
    }

    public static boolean isEmpty(String s) {
        if (s == null || "".equals(s) || "null".equals(s) || "\u0000".equals(s)) {
            return true;
        } else if ("".equals(s.trim())) {
            return true;
        }
        return false;
    }

    public static String getFileNameNoSuffix(String fileName) {
        Integer index = fileName.lastIndexOf('.');
        if (index == -1) {
            return fileName;
        }
        fileName = fileName.substring(0, index);
        return fileName;
    }

    public static String getFileNameSuffix(String fileName) {
        Integer index = fileName.lastIndexOf('.');
        if (index == -1) {
            return "";
        }
        String suffix = fileName.substring(index);
        return suffix;
    }



    public static String rename(String filename) {
        String fileNameReal = getFileNameNoSuffix(filename);
        String suffix = getFileNameSuffix(filename);
        StringBuilder sb = new StringBuilder(fileNameReal);
        sb.append("_").append(getRandomString(Constants.FIVE)).append('.').append(suffix);
        return sb.toString();
    }

    public static boolean pathIsOk(String filePath) {

        // Not null
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }

        // Not allowed to enter parent directory
        if (filePath.contains("..") || filePath.contains("./") || filePath.contains(".\\")) {
            return false;
        }

        // No invalid chars
        String invalidChars = "[<>\"|?*]";
        if (filePath.matches(".*" + invalidChars + ".*")) {
            return false;
        }

        return true;
    }
}
