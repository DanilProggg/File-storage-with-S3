package com.goodprod.project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
@Configuration
public class S3Config {
    @Value("${s3.ip}")
    private String ip;
    @Value("${s3.access.key}")
    private String accessKey;
    @Value("${s3.secret.key}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                .endpointOverride(URI.create(ip)) // Укажите ваш локальный адрес
                .region(Region.of("us-west-1")) // Укажите регион
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true) // Включите поддержку стиля доступа по пути
                        .build())
                .build();
    }
}

