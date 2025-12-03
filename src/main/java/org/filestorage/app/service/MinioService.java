package org.filestorage.app.service;

import io.minio.*;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.filestorage.app.exception.MinioOperationException;
import org.filestorage.app.exception.ResourceAlreadyExistException;
import org.filestorage.app.model.MinioResource;
import org.filestorage.app.repository.MinioRepository;
import org.filestorage.app.util.ResourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

    private final MinioRepository minioRepository;

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
        String prefixTo = constructUserPrefix(userId) + to;

        if(minioRepository.exists(prefixTo)){
            throw new ResourceAlreadyExistException("Resource already exists");
        }

        if(from.endsWith("/")){
            moveDirectory(from, to, userId);
        } else {
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

        String normalizedPath = path.equals("/") ? "" : path;
        String prefix = constructUserPrefix(userId) + normalizedPath;

        Iterable<Result<Item>> results = minioRepository.list(prefix, false);

        for (Result<Item> result : results) {
            Item item = minioRepository.extractItem(result);

            if (item.objectName().equals(prefix)) continue;

            MinioResource resource = new MinioResource();
            resource.setPath(path);

            if(item.objectName().endsWith("/")){
                resource.setName(extractDirectoryName(item.objectName()));
                resource.setType(ResourceType.DIRECTORY);
            } else {
                resource.setName(extractFileName(item.objectName()));
                resource.setSize(item.size());
                resource.setType(ResourceType.FILE);
            }

            resources.add(resource);
        }

        return resources;
    }

    public List<MinioResource> getResourcesByUser(Long userId, String query){

        List<MinioResource> resources = new ArrayList<>();

        Set<String> resourcesSet = new LinkedHashSet<>();
        String prefix = constructUserPrefix(userId);

        Iterable<Result<Item>> results = minioRepository.list(prefix, true);

        for (Result<Item> result : results) {
            Item item = minioRepository.extractItem(result);
            if (item.objectName().equals(prefix)) continue;

            int indexOfFirstSlash = item.objectName().indexOf("/");
            String path = item.objectName().substring(indexOfFirstSlash + 1, item.objectName().length());

            resourcesSet.add(path);

            if (!path.endsWith("/")) {
                path = path.substring(0, path.lastIndexOf('/') + 1);
            }

            while (path.contains("/")) {
                resourcesSet.add(path);
                path = path.substring(0, path.substring(0, path.length() - 1).lastIndexOf('/') + 1);
            }
        }

        for(String path : resourcesSet){
            MinioResource resource = getResource(path, userId);
            if(resource.getName().toLowerCase().contains(query.toLowerCase())) resources.add(resource);
        }

        return resources;
    }

    public void createDirectory(String path, Long userId){
        String prefix = constructUserPrefix(userId) + path;

        if(minioRepository.exists(prefix)){
            throw new ResourceAlreadyExistException("Directory " + path + " already exists");
        }

        minioRepository.putObject(prefix, new ByteArrayInputStream(new byte[0]), 0L);
    }

    private MinioResource getDirectory(String path){
        return new MinioResource(
                extractDirectoryPath(path),
                extractDirectoryName(path),
                null,
                ResourceType.DIRECTORY
        );
    }

    private MinioResource getFile(String path, Long userId){
        StatObjectResponse response = minioRepository.stat(constructUserPrefix(userId) + path);
        return new MinioResource(
                extractFilePath(path),
                extractFileName(path),
                response.size(),
                ResourceType.FILE
        );
    }

    private void deleteDirectory(String path, Long userId){

        Iterable<Result<Item>> results = minioRepository.list(constructUserPrefix(userId) + path, true);

        List<DeleteObject> objects = StreamSupport.stream(results.spliterator(), false)
                .map(minioRepository::extractItem)
                .map(Item::objectName)
                .map(DeleteObject::new)
                .collect(Collectors.toCollection(ArrayList::new));

        if(objects.isEmpty()){
            objects.add(new DeleteObject(constructUserPrefix(userId) + path));
        }

        minioRepository.removeObjects(objects);
    }

    private void deleteFile(String path, Long userId){
        minioRepository.removeObject(constructUserPrefix(userId) + path);
    }

    private StreamingResponseBody downloadDirectory(String path, Long userId){

        Iterable<Result<Item>> results = minioRepository.list(constructUserPrefix(userId) + path, true);

        StreamingResponseBody stream = outputStream -> {
            try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                String prefix = (constructUserPrefix(userId) + path);

                for (Result<Item> result : results) {
                    Item item = minioRepository.extractItem(result);
                    if (item.isDir()) continue;

                    String objectName = item.objectName();

                    String entryName = objectName.substring(prefix.length());
                    try (InputStream input = minioRepository.getObject(objectName)) {
                        ZipEntry entry = new ZipEntry(entryName);
                        zipOut.putNextEntry(entry);
                        input.transferTo(zipOut);
                        zipOut.closeEntry();
                    }

                }
            } catch (Exception e) {
                throw new MinioOperationException("Error zip processing " + path, e);
            }
        };

        return stream;
    }

    private StreamingResponseBody downloadFile(String path, Long userId){
        GetObjectResponse object = minioRepository.getObject(constructUserPrefix(userId) + path);
        return outputStream -> {
            try (InputStream input = object){
                input.transferTo(outputStream);
            }
        };
    }

    private void moveDirectory(String from, String to, Long userId){

        String source = constructUserPrefix(userId) + from;
        List<Item> items = new ArrayList<>();
        Iterable<Result<Item>> results = minioRepository.list(source, true);

        for (Result<Item> result : results) {
            items.add(minioRepository.extractItem(result));
        }

        for (Item item : items) {
            String target = item.objectName().replaceFirst(source, "");
            String targetPath = constructUserPrefix(userId) + to + target;
            minioRepository.copyObject(targetPath, item.objectName());
        }

        for (Item item : items) {
            minioRepository.removeObject(item.objectName());
        }
    }

    private void moveFile(String from, String to, Long userId){
        String sourcePath = constructUserPrefix(userId) + from;
        String targetPath = constructUserPrefix(userId) + to;

        minioRepository.copyObject(sourcePath, targetPath);
        minioRepository.removeObject(sourcePath);
    }

    private void uploadProcess(String path, Long userId, MultipartFile resource) {

        String prefix = constructUserPrefix(userId) + path + resource.getOriginalFilename();

        if(minioRepository.exists(prefix)) {
            throw new ResourceAlreadyExistException("Resource already exists");
        }

        if(resource.isEmpty()) {
            throw new MinioOperationException("Resource is empty");
        };

        try {
            minioRepository.putObject(prefix, resource.getInputStream(), resource.getSize());
        } catch (Exception e) {
            throw new MinioOperationException("Error uploading resource " + path, e);
        }

    }

    public void createUserPrefix(Long userId){
        String prefix = constructUserPrefix(userId);
        minioRepository.putObject(prefix, new ByteArrayInputStream(new byte[0]), 0L);
    }

    public String constructUserPrefix(Long userId){
        StringBuilder prefix = new StringBuilder();
        prefix.append(userPrefix);
        prefix.append(userId);
        prefix.append(userSuffix);
        return prefix.toString();
    }

    private String extractDirectoryName(String path){
        if(path.equals("/")){
            return path;
        } else {
            String normalizedPath = path.substring(0, path.length() - 1);
            int slashIndex = normalizedPath.lastIndexOf("/");
            return slashIndex > 0 ? normalizedPath.substring(slashIndex + 1) : normalizedPath;
        }
    }

    private String extractDirectoryPath(String path){
        long count = path.chars().filter(c -> c == '/').count();

        if(count <= 1) {
            return "/";
        } else {
            String normalizedPath = path.substring(0, path.length() - 1);
            int slashIndex = normalizedPath.lastIndexOf("/");
            return normalizedPath.substring(0, slashIndex + 1);
        }
    }

    private String extractFileName(String sourcePath){
        int slashIndex = sourcePath.lastIndexOf("/");
        return slashIndex < 1 ? sourcePath : sourcePath.substring(slashIndex + 1);
    }

    private String extractFilePath(String sourcePath){
        int slashIndex = sourcePath.lastIndexOf("/");
        return slashIndex < 1 ? "/" : sourcePath.substring(0, slashIndex + 1);
    }

}
