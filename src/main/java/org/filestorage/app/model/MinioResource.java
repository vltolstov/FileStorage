package org.filestorage.app.model;

import io.minio.MinioClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.filestorage.app.util.ResourceType;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MinioResource {

    private String path;

    private String name;

    private Long size;

    private ResourceType type;

}
