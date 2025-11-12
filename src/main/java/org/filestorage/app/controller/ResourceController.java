package org.filestorage.app.controller;

import lombok.RequiredArgsConstructor;
import org.filestorage.app.dto.ResourceResponse;
import org.filestorage.app.exception.PathNotValidException;
import org.filestorage.app.exception.ResourceNotFoundException;
import org.filestorage.app.mapper.ResourceDataResponseMapper;
import org.filestorage.app.model.MinioResource;
import org.filestorage.app.model.User;
import org.filestorage.app.service.MinioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ResourceController {

    private final MinioService minioService;
    private final ResourceDataResponseMapper resourceDataResponseMapper;

    @GetMapping("/resource")
    public ResponseEntity<ResourceResponse> getResourceData(@RequestParam String path, @AuthenticationPrincipal User user){
        pathValidation(path);
        prefixValidation(path, user.getId());

        MinioResource minioResource = minioService.getResource(path, user.getId());
        ResourceResponse resourceResponse = resourceDataResponseMapper.toResponse(minioResource);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resourceResponse);
    }

    private void pathValidation(String path) {
        if(path == null || path.isBlank()){
            throw new PathNotValidException("Path not valid");
        }
    }

    private void prefixValidation(String path, Long userId){
        if(path.endsWith("/")){
            if(!minioService.isFolderExist(path, userId)){
                throw new ResourceNotFoundException("Folder " + path + " not found");
            }
        } else {
            if(!minioService.isFileExist(path, userId)){
                throw new ResourceNotFoundException("File " + path + " not found");
            }
        }

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

}
