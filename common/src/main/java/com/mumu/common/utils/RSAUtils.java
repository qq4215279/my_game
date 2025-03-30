/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.common.utils;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * RSAUtils
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/30 14:27
 */
public class RSAUtils {
    /** 加密算法RSA */
    public static final String KEY_ALGORITHM = "RSA";
    /** 签名算法 */
    public static final String SIGNATURE_ALGORITHM = "MD5withRSA";
    /** 获取公钥的key */
    private static final String PUBLIC_KEY = "RSAPublicKey";
    /** 获取私钥的key */
    private static final String PRIVATE_KEY = "RSAPrivateKey";
    /** RSA最大加密明文大小 */
    private static final int MAX_ENCRYPT_BLOCK = 117;
    /** RSA最大解密密文大小 */
    private static final int MAX_DECRYPT_BLOCK = 128;

    /**
     * 生成密钥对(公钥和私钥)
     * @return java.util.Map<java.lang.String,java.lang.Object>
     * @date 2024/6/19 12:02
     */
    public static Map<String, Object> genKeyPair() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        keyPairGen.initialize(1024);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey)keyPair.getPublic();

        RSAPrivateKey privateKey = (RSAPrivateKey)keyPair.getPrivate();
        Map<String, Object> keyMap = new HashMap<String, Object>(2);
        keyMap.put(PUBLIC_KEY, publicKey);
        keyMap.put(PRIVATE_KEY, privateKey);
        return keyMap;
    }

    /**
     * 用私钥对信息生成数字签名
     * @param data 已加密数据
     * @param keyBytes privateKey 私钥
     * @return byte[]
     * @date 2024/6/19 12:02
     */
    public static byte[] sign(byte[] data, byte[] keyBytes) throws Exception {
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PrivateKey privateK = keyFactory.generatePrivate(pkcs8KeySpec);
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateK);
        signature.update(data);
        return signature.sign();
    }

    /**
     * 私钥解密
     * @param encryptedData 已加密数据
     * @param keyBytes privateKey 私钥
     * @return byte[]
     * @date 2024/6/19 12:03
     */
    public static byte[] decryptByPrivateKey(byte[] encryptedData, byte[] keyBytes) throws Exception {
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, privateK);
        int inputLen = encryptedData.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段解密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray();
        out.close();
        return decryptedData;
    }

    /**
     * 公钥解密
     * @param encryptedData 已加密数据
     * @param keyBytes publicKey 公钥
     * @return byte[]
     * @date 2024/6/19 12:03
     */
    public static byte[] decryptByPublicKey(byte[] encryptedData, byte[] keyBytes) throws Exception {
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key publicK = keyFactory.generatePublic(x509KeySpec);
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, publicK);
        int inputLen = encryptedData.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段解密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray();
        out.close();
        return decryptedData;
    }

    /**
     * 公钥加密
     * @param data 源数据
     * @param keyBytes publicKey 公钥
     * @return byte[]
     * @date 2024/6/19 12:04
     */
    public static byte[] encryptByPublicKey(byte[] data, byte[] keyBytes) throws Exception {
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key publicK = keyFactory.generatePublic(x509KeySpec);
        // 对数据加密
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.ENCRYPT_MODE, publicK);
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段加密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray();
        out.close();
        return encryptedData;
    }

    /**
     * 私钥加密
     * @param data 源数据
     * @param keyBytes privateKey 私钥
     * @return byte[]
     * @date 2024/6/19 12:04
     */
    public static byte[] encryptByPrivateKey(byte[] data, byte[] keyBytes) throws Exception {
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.ENCRYPT_MODE, privateK);
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段加密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray();
        out.close();
        return encryptedData;
    }

    /**
     * 获取公钥
     * @param keyMap 密钥对
     * @return byte[]
     * @date 2024/6/19 12:05
     */
    public static byte[] getPublicKey(Map<String, Object> keyMap) throws Exception {
        Key key = (Key)keyMap.get(PUBLIC_KEY);
        return key.getEncoded();
    }

    /**
     * 获取私钥
     * @param keyMap 密钥对
     * @return byte[]
     * @date 2024/6/19 12:04
     */
    public static byte[] getPrivateKey(Map<String, Object> keyMap) throws Exception {
        Key key = (Key)keyMap.get(PRIVATE_KEY);
        return key.getEncoded();
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 3; i++) {
            Map<String, Object> keyMap = RSAUtils.genKeyPair();
            byte[] pubKey = RSAUtils.getPublicKey(keyMap);

            Base64.Encoder encoder = Base64.getEncoder();
            byte[] encode = encoder.encode(pubKey);

            String pubKeyString = Base64Utils.encodeToString(pubKey);
            System.out.println(pubKeyString);
            byte[] privateKey = RSAUtils.getPrivateKey(keyMap);
            String str = "aaaaa";
            pubKey = Base64Utils.decodeFromString(pubKeyString);
            byte[] encrypt = RSAUtils.encryptByPublicKey(str.getBytes(), pubKey);

            byte[] dec = RSAUtils.decryptByPrivateKey(encrypt, privateKey);
            System.out.println(new String(dec));
        }
    }
}
