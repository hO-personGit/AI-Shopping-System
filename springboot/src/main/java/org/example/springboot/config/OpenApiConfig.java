package org.example.springboot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 接口文档配置。
 *
 * <p>启动后访问：
 * <ul>
 *   <li>Swagger UI：http://localhost:1234/swagger-ui.html</li>
 *   <li>OpenAPI JSON：http://localhost:1234/v3/api-docs</li>
 * </ul>
 * 接口文档随代码提交归档，满足项目对“Swagger 编写 API 文档”的要求。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI 智能商品销售系统 API")
                        .version("2.0.0")
                        .description("覆盖用户端、后台管理端与 AI 能力（智能导购 / 文案生成 / 销售分析）的 RESTful 接口文档。")
                        .contact(new Contact().name("AI-Shopping-System").email("546944475@qq.com"))
                        .license(new License().name("MIT")));
    }
}
