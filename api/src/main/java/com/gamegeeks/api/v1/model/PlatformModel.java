package com.gamegeeks.api.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlatformModel {

    @Schema(example = "6362927a289959266849759a")
    private String id;

    @Schema(example = "PC")
    private String name;

    @Schema(example = "")
    private String description;
}
