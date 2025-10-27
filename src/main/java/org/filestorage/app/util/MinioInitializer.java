package org.filestorage.app.util;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MinioInitializer {

    @Value("${minio.default.bucket}")
    private String defaultBucket;

    private final MinioClient minioClient;

    @PostConstruct
    public void init() throws Exception {
        boolean exist = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(defaultBucket).build()
        );

        if (!exist) {
            minioClient.makeBucket(
                MakeBucketArgs.builder().bucket(defaultBucket).build()
            );
        }
    }
}
