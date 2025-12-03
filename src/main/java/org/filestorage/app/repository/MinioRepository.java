package org.filestorage.app.repository;

import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.filestorage.app.exception.MinioOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MinioRepository {

    private final MinioClient minioClient;

    @Value("${minio.default.bucket}")
    private String defaultBucket;

    public GetObjectResponse getObject(String prefix) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(prefix)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioOperationException("Error getting object by prefix: " + prefix, e);
        }
    }

    public StatObjectResponse stat(String prefix) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(prefix)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioOperationException("Error stat object by prefix: " + prefix, e);
        }
    }

    public Iterable<Result<Item>> list(String prefix, boolean recursive) {
        try {
            return minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(defaultBucket)
                            .prefix(prefix)
                            .recursive(recursive)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioOperationException("Error list objects by prefix: " + prefix, e);
        }
    }

    public void removeObjects(List<DeleteObject> objects) {
        try {
            Iterable<Result<DeleteError>> results =
                    minioClient.removeObjects(
                            RemoveObjectsArgs.builder()
                                    .bucket(defaultBucket)
                                    .objects(objects)
                                    .build()
                    );

            for (Result<DeleteError> result : results) {
                DeleteError err = result.get();
                if (err != null) {
                    throw new MinioOperationException("MinIO delete error: " + err.objectName());
                }
            }

        } catch (Exception e) {
            throw new MinioOperationException("Error deleting multiple objects", e);
        }
    }

    public void removeObject(String prefix) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(prefix)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioOperationException("Error deleting object by prefix: " + prefix, e);
        }
    }

    public void copyObject(String target, String source){
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(target)
                            .source(
                                    CopySource.builder()
                                            .bucket(defaultBucket)
                                            .object(source)
                                            .build()
                            )
                            .build()
            );
        } catch (Exception e) {
            throw new MinioOperationException("Error copying object", e);
        }
    }

    public void putObject(String prefix, InputStream input, Long size){
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(prefix)
                            .stream(input, size, -1)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioOperationException("Error uploading file by prefix: " + prefix, e);
        }
    }

    public boolean exists(String prefix){
        try {
            if(prefix.endsWith("/")){
                return minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(defaultBucket)
                                .prefix(prefix)
                                .recursive(false)
                                .maxKeys(1)
                                .build()
                        )
                        .iterator()
                        .hasNext();
            } else {
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(defaultBucket)
                                .object(prefix)
                                .build()
                );
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public Item extractItem(Result<Item> result) {
        try {
            return result.get();
        } catch (Exception e) {
            throw new MinioOperationException("Error reading item", e);
        }
    }
}
