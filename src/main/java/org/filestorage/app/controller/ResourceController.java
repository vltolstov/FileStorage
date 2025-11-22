package org.filestorage.app.controller;

import io.minio.GetObjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.filestorage.app.dto.ResourceResponse;
import org.filestorage.app.exception.PathNotValidException;
import org.filestorage.app.exception.ResourceNotFoundException;
import org.filestorage.app.mapper.ResourceDataResponseMapper;
import org.filestorage.app.model.MinioResource;
import org.filestorage.app.model.User;
import org.filestorage.app.service.MinioService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequiredArgsConstructor
public class ResourceController {

    private final MinioService minioService;
    private final ResourceDataResponseMapper resourceDataResponseMapper;

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
        pathValidation(path);
        prefixValidation(path, user.getId());

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
    public ResponseEntity deleteResource(@RequestParam String path, @AuthenticationPrincipal User user){
        pathValidation(path);
        prefixValidation(path, user.getId());

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
        pathValidation(path);
        prefixValidation(path, user.getId());

        StreamingResponseBody streamResponse = minioService.downloadResource(path, user.getId());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(streamResponse);
    }

    private void pathValidation(String path) {
        if(path == null || path.isBlank()){
            throw new PathNotValidException("Path not valid");
        }
    }

    private void prefixValidation(String path, Long userId){
        if(path.endsWith("/")){
            if(!minioService.isFolderExist(path, userId)){
                throw new ResourceNotFoundException("Directory " + path + " not found");
            }
        } else {
            if(!minioService.isFileExist(path, userId)){
                throw new ResourceNotFoundException("File " + path + " not found");
            }
        }

    }

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
