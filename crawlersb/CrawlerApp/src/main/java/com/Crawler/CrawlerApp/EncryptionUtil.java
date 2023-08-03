package com.Crawler.CrawlerApp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;
import java.util.Scanner;

@Component
public class EncryptionUtil {
    private static final Logger logger = LogManager.getLogger(EncryptionUtil.class);
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String SECRET_KEY_ALGORITHM = "AES";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String AES_CIPHER = "AES/CBC/PKCS5Padding";

    public static String encrypt(String value, String encryptionKey) {
        try {
            byte[] keyBytes = getKeyBytes(encryptionKey);
            byte[] ivBytes = generateIV();

            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, SECRET_KEY_ALGORITHM);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance(AES_CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] encryptedValue = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] combinedBytes = new byte[ivBytes.length + encryptedValue.length];
            System.arraycopy(ivBytes, 0, combinedBytes, 0, ivBytes.length);
            System.arraycopy(encryptedValue, 0, combinedBytes, ivBytes.length, encryptedValue.length);

            return Base64.getEncoder().encodeToString(combinedBytes);
        } catch (Exception e) {
            logger.error("An error occurred during encryption: {}", e.getMessage());
        }
        return null;
    }

    public static String decrypt(String encryptedValue, String encryptionKey) {
        try {
            byte[] combinedBytes = Base64.getDecoder().decode(encryptedValue);

            byte[] ivBytes = new byte[16];
            System.arraycopy(combinedBytes, 0, ivBytes, 0, ivBytes.length);

            byte[] encryptedData = new byte[combinedBytes.length - ivBytes.length];
            System.arraycopy(combinedBytes, ivBytes.length, encryptedData, 0, encryptedData.length);

            byte[] keyBytes = getKeyBytes(encryptionKey);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, SECRET_KEY_ALGORITHM);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance(AES_CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] decryptedValue = cipher.doFinal(encryptedData);
            return new String(decryptedValue, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("An error occurred during decryption: {}", e.getMessage());
        }
        return null;
    }

    private static byte[] getKeyBytes(String encryptionKey) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] keyBytes = sha.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
        return truncateKeyBytes(keyBytes);
    }

    private static byte[] truncateKeyBytes(byte[] keyBytes) {
        byte[] truncatedKeyBytes = new byte[keyBytes.length];
        System.arraycopy(keyBytes, 0, truncatedKeyBytes, 0, keyBytes.length);
        return truncatedKeyBytes;
    }

    private static byte[] generateIV() {
        byte[] ivBytes = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(ivBytes);
        return ivBytes;
    }
}
