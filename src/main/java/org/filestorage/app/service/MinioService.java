package org.filestorage.app.service;

import io.minio.*;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.filestorage.app.dto.ResourceResponse;
import org.filestorage.app.exception.MinioOperationException;
import org.filestorage.app.exception.ResourceAlreadyExistException;
import org.filestorage.app.model.MinioResource;
import org.filestorage.app.util.ResourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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

    public StreamingResponseBody downloadResource(String path, Long userId){
        if(path.endsWith("/")){
            return downloadDirectory(path, userId);
        } else {
            return downloadFile(path, userId);
        }
    }

    public void moveResource(String from, String to, Long userId){
        if(from.endsWith("/")){
            if(isDirectoryExist(to, userId)){
                throw new ResourceAlreadyExistException("Directory " + to + " already exists");
            }
            moveDirectory(from, to, userId);
        } else {
            if(isFileExist(to, userId)){
                throw new ResourceAlreadyExistException("File " + to + " already exists");
            }
            moveFile(from, to, userId);
        }
    }

    public void uploadResource(String path, Long userId, MultipartFile[] resources) {
        for(MultipartFile resource : resources) {
            uploadProcess(path, userId, resource);
        }
    }

    public List<MinioResource> getResources(String path, Long userId){
        List<MinioResource> resources = new ArrayList<>();

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(defaultBucket)
                            .prefix(getUserPrefix(userId) + path)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();

                MinioResource resource = new MinioResource();
                if(item.objectName().endsWith("/")){
                    resource.setPath(path);
                    resource.setName(extractDirectoryName(item.objectName()));
                    resource.setType(ResourceType.DIRECTORY);
                } else {
                    resource.setPath(path);
                    resource.setName(extractFileName(item.objectName()));
                    resource.setSize(item.size());
                    resource.setType(ResourceType.FILE);
                }

                resources.add(resource);
            }
        } catch (Exception e) {
            throw new MinioOperationException("Error getting resources list from " + path);
        }

        return resources;
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

    private void moveDirectory(String from, String to, Long userId){

        String source = getUserPrefix(userId) + from;

        try {
            List<Item> items = new ArrayList<>();

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(defaultBucket)
                            .prefix(source)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                items.add(result.get());
            }

            for (Item item : items) {
                String target = item.objectName().replaceFirst(source, "");

                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(defaultBucket)
                                .object(getUserPrefix(userId) + to + target)
                                .source(
                                        CopySource.builder()
                                                .bucket(defaultBucket)
                                                .object(item.objectName())
                                                .build()
                                )
                                .build()
                );
            }

            for (Item item : items) {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(defaultBucket)
                                .object(item.objectName())
                                .build()
                );
            }
        } catch (Exception e) {
            throw new MinioOperationException("Error moving directory " + from + " to " + to);
        }
    }

    private void moveFile(String from, String to, Long userId){
        String sourcePath = getUserPrefix(userId) + from;
        String targetPath = getUserPrefix(userId) + to;
        try{
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(targetPath)
                            .source(
                                    CopySource.builder()
                                            .bucket(defaultBucket)
                                            .object(sourcePath)
                                            .build()
                            )
                            .build()
            );
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(sourcePath)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioOperationException("Error move file " + from + " to " + to);
        }
    }

    private void uploadProcess(String path, Long userId, MultipartFile resource){

        String target = path + resource.getOriginalFilename();

        if(isFileExist(target, userId)) {
            throw new ResourceAlreadyExistException("File already exists");
        }
        if(isDirectoryExist(target, userId)) {
            throw new ResourceAlreadyExistException("Directory already exists");
        }
        if(resource.isEmpty()) {
            throw new MinioOperationException("Resource is empty");
        };

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(getUserPrefix(userId) + target)
                            .stream(resource.getInputStream(), resource.getSize(), -1)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioOperationException("Error uploading file " + path);
        }
    }

    public boolean isDirectoryExist(String path, Long userId){
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

    //Папки
    //
    //Получение информации о содержимом папки
    //
    //GET /directory?path=$path

    //Создание / аплоад пустой папки.
    //
    //POST /directory?path=$path

}
