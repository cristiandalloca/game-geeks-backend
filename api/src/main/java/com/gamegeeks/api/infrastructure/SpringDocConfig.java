package com.gamegeeks.api.infrastructure;

import com.gamegeeks.api.exception.model.ProblemModel;
import com.gamegeeks.api.exception.model.ProblemType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
@SecurityScheme(name = "security_auth",
        type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(clientCredentials = @OAuthFlow(
                authorizationUrl = "${springdoc.oAuthFlow.authorization-url}",
                tokenUrl = "${springdoc.oAuthFlow.token-url}"
        )))
public class SpringDocConfig implements WebMvcConfigurer {

    private static final String BAD_REQUEST_RESPONSE = "BadRequestResponse";
    private static final String NOT_FOUND_RESPONSE = "NotFoundResponse";
    private static final String NOT_ACCEPTABLE_RESPONSE = "NotAcceptableResponse";
    private static final String INTERNAL_SERVER_ERROR_RESPONSE = "InternalServerErrorResponse";
    private static final String UNAUTHORIZED_RESPONSE = "UnauthorizedResponse";
    private static final String SWAGGER_UI_HTML = "/swagger-ui.html";

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", SWAGGER_UI_HTML);
        registry.addRedirectViewController("/swagger", SWAGGER_UI_HTML);
        registry.addRedirectViewController("/swagger-ui", SWAGGER_UI_HTML);
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Game Geeks API")
                        .version("v1")
                        .description("REST API do Game Geeks"))
                .components(new Components()
                        .responses(generateResponses()));
    }

    @Bean
    public OpenApiCustomiser openApiCustomiser() {
        return openApi -> {

            openApi.getComponents().getSchemas().putAll(generateSchemas());
            openApi.getPaths()
                    .values()
                    .forEach(pathItem -> pathItem.readOperationsMap()
                            .forEach((httpMethod, operation) -> {
                                ApiResponses responses = operation.getResponses();
                                switch (httpMethod) {
                                    case GET, POST, PUT -> {
                                        this.addApiResponseConsideringResponseExisting(responses, String.valueOf(HttpStatus.BAD_REQUEST.value()), new ApiResponse().$ref(BAD_REQUEST_RESPONSE));
                                        this.addApiResponseConsideringResponseExisting(responses, String.valueOf(HttpStatus.NOT_FOUND.value()), new ApiResponse().$ref(NOT_FOUND_RESPONSE));
                                        this.addApiResponseConsideringResponseExisting(responses, String.valueOf(HttpStatus.NOT_ACCEPTABLE.value()), new ApiResponse().$ref(NOT_ACCEPTABLE_RESPONSE));
                                        this.addApiResponseConsideringResponseExisting(responses, String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), new ApiResponse().$ref(INTERNAL_SERVER_ERROR_RESPONSE));
                                        this.addApiResponseConsideringResponseExisting(responses, String.valueOf(HttpStatus.UNAUTHORIZED.value()), new ApiResponse().$ref(UNAUTHORIZED_RESPONSE));
                                    }
                                    default -> this.addApiResponseConsideringResponseExisting(responses, String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), new ApiResponse().$ref(INTERNAL_SERVER_ERROR_RESPONSE));
                                }
                            })
                    );
        };
    }

    private void addApiResponseConsideringResponseExisting(ApiResponses responses, String name, ApiResponse apiResponse) {
        if (responses.containsKey(name)) {
            ApiResponse response = responses.get(name);
            if (!String.valueOf(HttpStatus.OK.value()).equals(name)) {
                response.setContent(this.getContentProblemModelDefault());
            }
            responses.put(name, response);
        } else
            responses.put(name, apiResponse);
    }

    private Map<String, ApiResponse> generateResponses() {
        final var responses = new HashMap<String, ApiResponse>();

        Content content = this.getContentProblemModelDefault();

        responses.put(BAD_REQUEST_RESPONSE, new ApiResponse()
                .description(ProblemType.INVALID_DATA.getTitle())
                .content(content));

        responses.put(NOT_FOUND_RESPONSE, new ApiResponse()
                .description(ProblemType.RESOURCE_NOT_FOUND.getTitle())
                .content(content));

        responses.put(NOT_ACCEPTABLE_RESPONSE, new ApiResponse()
                .description(ProblemType.NOT_ACCEPTABLE_RESPONSE.getTitle())
                .content(content));

        responses.put(INTERNAL_SERVER_ERROR_RESPONSE, new ApiResponse()
                .description(ProblemType.SYSTEM_ERROR.getTitle())
                .content(content));

        responses.put(UNAUTHORIZED_RESPONSE, new ApiResponse()
                .description("Unauthorized"));

        return responses;
    }

    private Content getContentProblemModelDefault() {
        return new Content()
                .addMediaType(APPLICATION_JSON_VALUE,
                        new MediaType().schema(new Schema<ProblemModel>().$ref(ProblemModel.class.getSimpleName())));
    }

    private Map<String, Schema> generateSchemas() {
        final var schemas = new HashMap<String, Schema>();

        Map<String, Schema> problemSchema = ModelConverters.getInstance().read(ProblemModel.class);
        Map<String, Schema> problemObjectSchema = ModelConverters.getInstance().read(ProblemModel.Field.class);

        schemas.putAll(problemSchema);
        schemas.putAll(problemObjectSchema);

        return schemas;
    }
}
