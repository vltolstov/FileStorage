package org.filestorage.app.mapper;

import org.filestorage.app.dto.DirectoryResourceResponse;
import org.filestorage.app.dto.FileResourceResponse;
import org.filestorage.app.dto.ResourceResponse;
import org.filestorage.app.model.MinioResource;
import org.filestorage.app.util.ResourceType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ResourceDataResponseMapper {

    default ResourceResponse toResponse(MinioResource resource){
        ResourceType type = resource.getType();
        if(type == ResourceType.FILE){
            FileResourceResponse response = new FileResourceResponse();
            response.setPath(resource.getPath());
            response.setName(resource.getName());
            response.setSize(resource.getSize());
            response.setType(String.valueOf(resource.getType()));
            return response;
        } else {
            DirectoryResourceResponse response = new DirectoryResourceResponse();
            response.setPath(resource.getPath());
            response.setName(resource.getName());
            response.setType(String.valueOf(resource.getType()));
            return response;
        }
    }

}
