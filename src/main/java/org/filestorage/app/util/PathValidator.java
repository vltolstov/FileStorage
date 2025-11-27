package org.filestorage.app.util;

import lombok.RequiredArgsConstructor;
import org.filestorage.app.exception.PathNotValidException;
import org.filestorage.app.exception.ResourceNotFoundException;
import org.filestorage.app.service.MinioService;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PathValidator {

    private final MinioService minioService;

    public void pathValidation(String path) {
        if(path == null || path.isBlank()){
            throw new PathNotValidException("Path not valid");
        }
    }

    public void directoryPathValidation(String path) {
        if(path == null || path.isBlank() || !path.endsWith("/")){
            throw new PathNotValidException("Path not valid");
        }
    }

    public void prefixValidation(String path, Long userId){
        if(path.endsWith("/")){
            if(!minioService.isDirectoryExist(path, userId)){
                throw new ResourceNotFoundException("Directory " + path + " not found");
            }
        } else {
            if(!minioService.isFileExist(path, userId)){
                throw new ResourceNotFoundException("File " + path + " not found");
            }
        }
    }

}
