package org.filestorage.app.util;

import lombok.RequiredArgsConstructor;
import org.filestorage.app.exception.PathNotValidException;
import org.filestorage.app.exception.ResourceNotFoundException;
import org.filestorage.app.repository.MinioRepository;
import org.filestorage.app.service.MinioService;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PathValidator {

    private final MinioService minioService;
    private final MinioRepository minioRepository;

    public void pathValidation(String path) {
        if (!isValidCommon(path)) {
            throw new PathNotValidException("Path not valid");
        }
    }

    private boolean isValidCommon(String path) {

        if (path.contains("//")) {
            return false;
        }

        if (!path.matches("^[\\p{L}0-9._\\-/ ]+$")) {
            return false;
        }

        String[] segments = path.split("/");

        for (String segment : segments) {
            if (segment.isBlank()) {
                return false;
            }
            if (segment.equals(".") || segment.equals("..")) {
                return false;
            }
        }

        return true;
    }

    public void prefixValidation(String path, Long userId){
        String prefix = minioService.constructUserPrefix(userId) + path;
        if(!minioRepository.exists(prefix)){
            throw new ResourceNotFoundException("Resource " + path + " not found");
        }
    }

}
