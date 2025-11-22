package org.filestorage.app.service;

import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.filestorage.app.exception.MinioOperationException;
import org.filestorage.app.model.MinioResource;
import org.filestorage.app.util.ResourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    public void deleteResource(String path, Long userId){
        if(path.endsWith("/")){
            deleteDirectory(path, userId);
        } else {
            deleteFile(path, userId);
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

    private void deleteDirectory(String path, Long userId){
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(defaultBucket)
                            .prefix(getUserPrefix(userId) + path)
                            .recursive(true)
                            .build()
            );

            List<DeleteObject> objects = StreamSupport.stream(results.spliterator(), false)
                    .map(result -> {
                        try {
                            return new DeleteObject(result.get().objectName());
                        } catch (Exception e) {
                            throw new MinioOperationException("Error collect files for deleting from " + path);
                        }
                    })
                    .toList();

            if(objects.isEmpty()){
                objects.add(new DeleteObject(getUserPrefix(userId) + path));
            }

            minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(defaultBucket)
                            .objects(objects)
                            .build()
            ).forEach(result -> {
                try { result.get(); } catch (Exception e) {
                    throw new MinioOperationException("Error deleting files from " + path);
                }
            });

        } catch (Exception e) {
            throw new MinioOperationException("Error deleting directory " + path);
        }
    }

    private void deleteFile(String path, Long userId){
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(getUserPrefix(userId) + path)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioOperationException("Error deleting file " + path);
        }
    }

    public StreamingResponseBody downloadResource(String path, Long userId){
        if(path.endsWith("/")){
            return downloadDirectory(path, userId);
        } else {
            return downloadFile(path, userId);
        }
    }

    private StreamingResponseBody downloadDirectory(String path, Long userId){
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(defaultBucket)
                            .prefix(getUserPrefix(userId) + path)
                            .recursive(true)
                            .build()
            );

            StreamingResponseBody stream = outputStream -> {
                try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                    for (Result<Item> result : results) {
                        Item item = result.get();
                        if (item.isDir()) continue;

                        try (InputStream input = minioClient.getObject(
                                GetObjectArgs.builder()
                                        .bucket(defaultBucket)
                                        .object(item.objectName())
                                        .build()
                        )) {
                            ZipEntry entry = new ZipEntry(item.objectName().substring((getUserPrefix(userId) + path).length()));
                            zipOut.putNextEntry(entry);
                            input.transferTo(zipOut);
                            zipOut.closeEntry();
                        }
                    }
                } catch (Exception e) {
                    throw new MinioOperationException("Error zip processing " + path);
                }
            };
            return stream;
        } catch (Exception e) {
            throw new MinioOperationException("Error downloading directory " + path);
        }
    }

    private StreamingResponseBody downloadFile(String path, Long userId){
        try {
            GetObjectResponse object = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(getUserPrefix(userId) + path)
                            .build()
            );

            return outputStream -> {
                try (InputStream input = object){
                    input.transferTo(outputStream);
                }
            };
        } catch (Exception e) {
            throw new MinioOperationException("Error downloading file " + path);
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
