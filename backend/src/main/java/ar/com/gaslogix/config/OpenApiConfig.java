package ar.com.gaslogix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Documentación OpenAPI (Swagger). UI en /swagger-ui.html, definición en /v3/api-docs.
 * Todos los endpoints requieren el token JWT en el header Authorization: Bearer <token>.
 */
@Configuration
@OpenAPIDefinition(security = @SecurityRequirement(name = "Bearer Authentication"))
@SecurityScheme(
		name = "Bearer Authentication",
		type = SecuritySchemeType.HTTP,
		bearerFormat = "JWT",
		scheme = "bearer",
		in = SecuritySchemeIn.HEADER)
public class OpenApiConfig {

	@Bean
	public OpenAPI openApi() {
		return new OpenAPI()
				.info(new Info()
						.title("GasLogix API")
						.description("Backend para la gestión de órdenes de carga de gas licuado de petróleo (GLP) - Ingeniería Web 3")
						.version("v1.0"));
	}
}
