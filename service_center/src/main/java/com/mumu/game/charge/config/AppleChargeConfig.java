package com.mumu.game.charge.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import com.apple.itunes.storekit.client.AppStoreServerAPIClient;
import com.apple.itunes.storekit.model.Environment;
import com.apple.itunes.storekit.verification.SignedDataVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;


import lombok.Data;

/**
 * AppleChargeConfig
 * 苹果充值配置
 * @author liuzhen
 * @version 1.0.0 2025/1/16 19:30
 */
@Data
@Component
@Configuration
public class AppleChargeConfig {
  /** BUNDLE_ID */
  @Value("${charge.apple.bundle-id:}")
  private String bundleId = "";
  /** APPAPPLE_ID */
  @Value("${charge.apple.app-apple-id}")
  private long appAppleId = 0L;
  /** 私钥id */
  @Value("${charge.apple.private-key-id}")
  private String privateKeyId = "";
  /** 发行人id */
  @Value("${charge.apple.issuer-id}")
  private String issuerId = "";

  /**
   * 正式服苹果服务器调用client
   * @return com.apple.itunes.storekit.client.AppStoreServerAPIClient
   * @since 2025/1/17 13:59
   */
  @Bean
  public AppStoreServerAPIClient prodClient() {
    try {
      return new AppStoreServerAPIClient(getApplePaySigningKey(),
          privateKeyId, issuerId, bundleId, Environment.PRODUCTION);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 沙河苹果服务器调用client
   * @return com.apple.itunes.storekit.client.AppStoreServerAPIClient
   * @since 2025/1/17 13:59
   */
  @Bean
  public AppStoreServerAPIClient sandboxClient() {
    try {
      return new AppStoreServerAPIClient(getApplePaySigningKey(),
          privateKeyId, issuerId, bundleId, Environment.SANDBOX);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 获取苹果签名秘钥
   * @return java.lang.String
   * @since 2025/1/17 14:33
   */
  private String getApplePaySigningKey() throws IOException {
    try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream(
        "certs/SubscriptionKey_9KMRKCM7XS.p8")) {
      return new String(stream.readAllBytes());
    }
  }

  /**
   * 获取苹果正式服签名验证对象
   * @return com.apple.itunes.storekit.verification.SignedDataVerifier
   * @since 2025/6/6 13:59
   */
  @Bean
  public SignedDataVerifier prodSignedDataVerifier() {
    try {
      Set<InputStream> rootCertificates = Set.of(
          new ByteArrayInputStream(readBytes("certs/AppleRootCA-G3.cer")),
          new ByteArrayInputStream(readBytes("certs/AppleWWDRCAG8.cer")));
      return new SignedDataVerifier(rootCertificates, getBundleId(),
          getAppAppleId(), Environment.PRODUCTION, true);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 获取苹果沙河签名验证对象
   * @return com.apple.itunes.storekit.verification.SignedDataVerifier
   * @since 2025/6/6 14:00
   */
  @Bean
  public SignedDataVerifier sandboxSignedDataVerifier() {
    try {
      Set<InputStream> rootCertificates = Set.of(
          new ByteArrayInputStream(readBytes("certs/AppleRootCA-G3.cer")),
          new ByteArrayInputStream(readBytes("certs/AppleWWDRCAG8.cer")));
      return new SignedDataVerifier(rootCertificates, getBundleId(),
          getAppAppleId(), Environment.SANDBOX, true);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 读文件内容
   * @return byte[]
   * @since 2025/1/15 15:45
   */
  private byte[] readBytes(String filePath) throws IOException {
    try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream(filePath)) {
      if (stream != null) {
        return stream.readAllBytes();
      }
      return new byte[0];
    }
  }
}
