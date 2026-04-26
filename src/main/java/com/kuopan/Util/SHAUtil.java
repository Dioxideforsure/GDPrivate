package com.kuopan.Util;

import com.kuopan.Entity.enums.SHAEnum;
import com.kuopan.Exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
public class SHAUtil {

    // SHA Encrypt
    public static String SHAEncrypt(String SHAType, String Message) {
        if (!SHAEnum.hasType(SHAType)) {
            throw new BusinessException("不支持此类加密方式");
        }
        try {
            MessageDigest md = MessageDigest.getInstance(SHAType);
            md.update(Message.getBytes(StandardCharsets.UTF_8));
            byte[] hashValue = md.digest();

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < hashValue.length; i++) {
                result.append(String.format("%02x", hashValue[i]));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Encryption process error: {}", e.getMessage());
            throw new BusinessException("加密失败");
        }
    }

    public static String SHAEncrypt(SHAEnum SHAEnum, String Message) {
        return SHAEncrypt(SHAEnum.getSHAType(), Message);
    }

    public static String SHA256Encrypt(String Message) {
        return SHAEncrypt(SHAEnum.SHA_256, Message);
    }

    public static void main(String[] args) {
        System.out.println(SHAEncrypt(SHAEnum.SHA_256, "114514hajimi"));
    }

}
