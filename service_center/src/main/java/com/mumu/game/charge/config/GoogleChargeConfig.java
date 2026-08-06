package com.mumu.game.charge.config;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;

import lombok.Data;

/**
 * GoogleChargeConfig
 * 谷歌充值配置
 * @author liuzhen
 * @version 1.0.0 2025/2/12 15:16
 */
@Data
@Component
@Configuration
public class GoogleChargeConfig {
  /** packageName */
  @Value("${charge.google.package-name:}")
  private String packageName = "";
  /** CLIENT_ID */
  @Value("${charge.google.client-id:}")
  private String clientId = "";
  /** ACCOUNT_ID */
  @Value("${charge.google.account-id:}")
  private String accountId = "";
  /** CLIENT_SECRET */
  @Value("${charge.google.client-secret}")
  private String clientSecret = "";
  /** REDIRECT_URI */
  @Value("${charge.google.redirect-uri:}")
  private String redirectUri = "";

  /**
   * 获取 publisher 对象
   * @return com.google.api.services.androidpublisher.AndroidPublisher
   * @since 2025/2/13 14:38
   */
  public AndroidPublisher getPublisher3() {
    // 使用服务帐户Json文件获取Google凭据
    List<String> scopes = new ArrayList<>();
    scopes.add(AndroidPublisherScopes.ANDROIDPUBLISHER);
    ResourceLoader resourceLoader = new DefaultResourceLoader();
    Resource resource = resourceLoader.getResource("certs/google/halabaloot-45d5b03e9db4.json");
    GoogleCredential credential = null;
    try {
      credential = GoogleCredential.fromStream(resource.getInputStream())
          .createScoped(scopes);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // 使用谷歌凭据和收据从谷歌获取购买信息
    HttpTransport httpTransport = null;
    try {
      httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    } catch (GeneralSecurityException | IOException e) {
      throw new RuntimeException(e);
    }

    // JacksonFactory jsonFactory = new JacksonFactory();

    return new AndroidPublisher.Builder(httpTransport, JacksonFactory.getDefaultInstance(), credential)
        .setApplicationName("هلا بلوت").build();
  }

}
