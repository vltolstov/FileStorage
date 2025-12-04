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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ResourceController {

    private final MinioService minioService;
    private final ResourceDataResponseMapper resourceDataResponseMapper;
    private final PathValidator pathValidator;

    @Operation(summary = "Получение информации о ресурсе", description = "Возвращает путь, имя, размер(для файла), тип ресурса")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ресурс найден, информация получена"),
            @ApiResponse(responseCode = "400", description = "Ошибки валидации"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден"),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка")
    })
    @GetMapping("/resource")
    public ResponseEntity<ResourceResponse> getResourceData(@RequestParam String path, @AuthenticationPrincipal User user){
        pathValidator.pathValidation(path);
        pathValidator.prefixValidation(path, user.getId());

        MinioResource minioResource = minioService.getResource(path, user.getId());
        ResourceResponse resourceResponse = resourceDataResponseMapper.toResponse(minioResource);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resourceResponse);
    }

    @Operation(summary = "Удаление ресурса", description = "Возвращает ответ с пустым телом. Код 204")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ресурс найден и удален"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден"),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка")
    })
    @DeleteMapping("/resource")
    public ResponseEntity<ResourceResponse> deleteResource(@RequestParam String path, @AuthenticationPrincipal User user){
        pathValidator.pathValidation(path);
        pathValidator.prefixValidation(path, user.getId());

        minioService.deleteResource(path, user.getId());

        return ResponseEntity
                .noContent()
                .build();
    }

    @Operation(summary = "Скачивание ресурса", description = "Возвращает бинарное содержимое с Content-Type: application/octet-stream")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ресурс найден. Скачивание"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден"),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка")
    })
    @GetMapping("/resource/download")
    public ResponseEntity<StreamingResponseBody> downloadResource(@RequestParam String path, @AuthenticationPrincipal User user){
        pathValidator.pathValidation(path);
        pathValidator.prefixValidation(path, user.getId());

        StreamingResponseBody streamResponse = minioService.downloadResource(path, user.getId());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(streamResponse);
    }

    @Operation(summary = "Перемещение или переименование ресурса", description = "Возвращает путь, имя, размер(для файла), тип ресурса")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ресурс перемещен/переименован"),
            @ApiResponse(responseCode = "400", description = "Ошибки валидации"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден"),
            @ApiResponse(responseCode = "409", description = "Такой ресурс уже содержится по данному пути"),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка")
    })
    @GetMapping("/resource/move")
    public ResponseEntity<ResourceResponse> moveResource(@RequestParam String from, @RequestParam String to, @AuthenticationPrincipal User user){
        pathValidator.pathValidation(from);
        pathValidator.pathValidation(to);
        pathValidator.prefixValidation(from, user.getId());

        minioService.moveResource(from, to, user.getId());
        MinioResource minioResource = minioService.getResource(to, user.getId());
        ResourceResponse resourceResponse = resourceDataResponseMapper.toResponse(minioResource);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resourceResponse);
    }

    @Operation(summary = "Загрузка ресурса", description = "Возвращает коллекцию ресурсов в формате путь, имя, размер, тип")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ресурс загружен"),
            @ApiResponse(responseCode = "400", description = "Ошибки валидации"),
            @ApiResponse(responseCode = "409", description = "Такой ресурс уже содержится по данному пути"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка")
    })
    @PostMapping("/resource")
    public ResponseEntity<List<ResourceResponse>> uploadResource(@RequestParam String path, @RequestParam MultipartFile[] resources, @AuthenticationPrincipal User user) {
        pathValidator.pathValidation(path);

        minioService.uploadResource(path, user.getId(), resources);
        List<MinioResource> uploadedResources = minioService.getResources(path, user.getId());

        List<ResourceResponse> resultList = uploadedResources.stream()
                .map(resourceDataResponseMapper::toResponse)
                .toList();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resultList);
    }

}
