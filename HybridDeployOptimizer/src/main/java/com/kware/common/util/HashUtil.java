package com.kware.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class HashUtil {
    

    public static String getMD5Hash(String input) {
        try {
            // MD5 MessageDigest 인스턴스 생성
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 입력 문자열을 바이트 배열로 변환
            byte[] messageDigest = md.digest(input.getBytes());
            // 바이트 배열을 16진수 문자열로 변환
            return byteArrayToHex(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String byteArrayToHex(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}