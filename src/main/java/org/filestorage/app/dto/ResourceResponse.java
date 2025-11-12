package org.filestorage.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public abstract class ResourceResponse {
    private String path;

    private String name;

    private String type;
}
