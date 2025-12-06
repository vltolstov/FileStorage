package org.filestorage.app.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String minioEndpoint;

    @Value("${minio.access-key}")
    private String userName;

    @Value("${minio.secret-key}")
    private String password;

    @Bean
    public MinioClient minioClient() {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(userName, password)
                .build();
        return minioClient;
    }
}
