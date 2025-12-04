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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import static org.filestorage.app.util.QueryValidator.validate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resource")
public class SearchResourceController {

    private final MinioService minioService;
    private final ResourceDataResponseMapper resourceDataResponseMapper;

    @Operation
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Возвращает List ресурсов в формате имя, путь, размер, тип"),
            @ApiResponse(responseCode = "400", description = "Ошибки валидации"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка")
    })
    @GetMapping("/search")
    public List<ResourceResponse> searchResource(@RequestParam String query, @AuthenticationPrincipal User user) {
        validate(query);

        List<MinioResource> resources = minioService.getResourcesByUser(user.getId(), query);

        List<ResourceResponse> resultList = resources.stream()
                .map(resourceDataResponseMapper::toResponse)
                .toList();

        return resultList;
    }

}
