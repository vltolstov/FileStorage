package org.filestorage.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.filestorage.app.dto.ResourceResponse;
import org.filestorage.app.mapper.ResourceDataResponseMapper;
import org.filestorage.app.model.MinioResource;
import org.filestorage.app.model.User;
import org.filestorage.app.service.MinioService;
import org.filestorage.app.util.PathValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class DirectoryController {

    private final MinioService minioService;
    private final PathValidator pathValidator;
    private final ResourceDataResponseMapper resourceDataResponseMapper;

    @Operation
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Возвращает путь, имя, тип ресурса"),
            @ApiResponse(responseCode = "400", description = "Ошибки валидации"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Директория не найдена"),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка")
    })
    @GetMapping("/directory")
    public ResponseEntity<List<ResourceResponse>> getDirectory(@RequestParam String path, @AuthenticationPrincipal User user) {
        pathValidator.directoryPathValidation(path);
        pathValidator.prefixValidation(path, user.getId());

        List<MinioResource> resources = minioService.getResources(path, user.getId());

        List<ResourceResponse> resultList = resources.stream()
                .map(resourceDataResponseMapper::toResponse)
                .toList();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resultList);
    }

    @PostMapping("/directory")
    public void postDirectory(@RequestParam String path, @AuthenticationPrincipal User user) {
        pathValidator.pathValidation(path);

        //Создание / аплоад пустой папки.
        //
        //POST /directory?path=$path
    }
}
