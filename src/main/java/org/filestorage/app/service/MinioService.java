package org.filestorage.app.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.filestorage.app.exception.MinioOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
@RequiredArgsConstructor
public class MinioService {

    @Value("${minio.default.bucket}")
    private String defaultBucket;

    private final MinioClient minioClient;

    public void createUserPrefix(Long userId){
        try {
            String prefix = "user-" + userId + "-files/";
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(prefix)
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .build()
            );
        } catch (Exception exception) {
            throw new MinioOperationException("Error creating prefix for user " + userId);
        }
    }

}
