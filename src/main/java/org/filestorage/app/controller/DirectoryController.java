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
import org.filestorage.app.util.PathNormalizer;
import org.filestorage.app.util.PathValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
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
        path = PathNormalizer.normalize(path);
        pathValidator.pathValidation(path);
        pathValidator.prefixValidation(path, user.getId());

        List<MinioResource> resources = minioService.getResources(path, user.getId());

        List<ResourceResponse> resultList = resources.stream()
                .map(resourceDataResponseMapper::toResponse)
                .toList();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resultList);
    }

    @Operation
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Возвращает путь, имя папки и тип"),
            @ApiResponse(responseCode = "400", description = "Ошибки валидации"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Родительская папка не существует"),
            @ApiResponse(responseCode = "409", description = "Папка уже существует"),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка")
    })
    @PostMapping("/directory")
    public ResponseEntity<ResourceResponse> postDirectory(@RequestParam String path, @AuthenticationPrincipal User user) {
        path = PathNormalizer.normalize(path);
        pathValidator.pathValidation(path);

        minioService.createDirectory(path, user.getId());
        MinioResource resource = minioService.getResource(path, user.getId());
        ResourceResponse resourceResponse = resourceDataResponseMapper.toResponse(resource);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resourceResponse);
    }
}