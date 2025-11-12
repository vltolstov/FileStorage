package org.filestorage.app.service;

import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.filestorage.app.exception.MinioOperationException;
import org.filestorage.app.model.MinioResource;
import org.filestorage.app.util.ResourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MinioService {

    @Value("${minio.default.bucket}")
    private String defaultBucket;

    @Value("${minio.user.prefix}")
    private String userPrefix;

    @Value("${minio.user.suffix}")
    private String userSuffix;

    private final MinioClient minioClient;

    public MinioResource getResource(String path, Long userId){
        if(path.endsWith("/")){
            return getDirectory(path);
        } else {
            return getFile(path, userId);
        }
    }

    private MinioResource getDirectory(String path){
        return new MinioResource(
                path,
                extractDirectoryName(path),
                null,
                ResourceType.DIRECTORY
        );
    }

    private MinioResource getFile(String path, Long userId){
        try{
            StatObjectResponse response = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(getUserPrefix(userId) + path)
                            .build()
            );
            return new MinioResource(
                    extractFilePath(path),
                    extractFileName(path),
                    response.size(),
                    ResourceType.FILE
            );
        } catch (Exception e) {
            throw new MinioOperationException("Error getting file " + path);
        }
    }

    private List<MinioResource> getDirectoryResources(String path, Long userId){
        return List.of(new MinioResource());
    }

    public boolean isFolderExist(String path, Long userId){
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(defaultBucket)
                            .prefix(getUserPrefix(userId) + path)
                            .recursive(false)
                            .maxKeys(1)
                            .build()
            );
            return results.iterator().hasNext();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isFileExist(String path, Long userId){
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(getUserPrefix(userId) + path)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void createUserPrefix(Long userId){
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(getUserPrefix(userId))
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .build()
            );
        } catch (Exception exception) {
            throw new MinioOperationException("Error creating prefix for user " + userId);
        }
    }

    private String getUserPrefix(Long userId){
        StringBuilder prefix = new StringBuilder();
        prefix.append(userPrefix);
        prefix.append(userId);
        prefix.append(userSuffix);
        return prefix.toString();
    }

    private String extractDirectoryName(String path){
        String normalizedPath = path.substring(0, path.length() - 1);
        int slashIndex = normalizedPath.lastIndexOf("/");
        return slashIndex > 0 ? normalizedPath.substring(slashIndex + 1) : normalizedPath;
    }

    private String extractFileName(String sourcePath){
        int slashIndex = sourcePath.lastIndexOf("/");
        return slashIndex < 1 ? sourcePath : sourcePath.substring(slashIndex + 1);
    }

    private String extractFilePath(String sourcePath){
        int slashIndex = sourcePath.lastIndexOf("/");
        return slashIndex < 1 ? "/" : sourcePath.substring(0, slashIndex + 1);
    }

    //Удаление ресурса
    //
    //DELETE /resource?path=$path

    //Скачивание ресурса
    //
    //GET /resource/download?path=$path

    //Переименование/перемещение ресурса
    //
    //GET /resource/move?from=$from&to=$to

    //Поиск
    //
    //GET /resource/search?query=$query

    //Аплоад
    //
    //POST resource?path=$path

    //Папки
    //
    //Получение информации о содержимом папки
    //
    //GET /directory?path=$path

    //Создание / аплоад пустой папки.
    //
    //POST /directory?path=$path

}
