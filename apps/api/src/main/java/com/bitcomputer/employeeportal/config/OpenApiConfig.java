package com.bitcomputer.employeeportal.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.PasswordSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI employeePortalOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("사내 직원 관리 시스템 API")
                        .version("1.0.0")
                        .description("직원과 관리자를 위한 내부 API입니다. 보호 API는 JSESSIONID 세션 쿠키가 필요합니다. "
                                + "POST·PUT 요청은 GET /api/auth/csrf 응답의 headerName과 token을 같은 이름의 헤더로 전달해야 합니다."))
                .components(new Components().addSecuritySchemes("sessionCookie", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("JSESSIONID")
                        .description("로그인 성공 후 서버가 발급하는 세션 쿠키")))
                .addTagsItem(new Tag().name("인증").description("로그인, 로그아웃과 현재 인증 상태"))
                .addTagsItem(new Tag().name("직원 본인 정보").description("로그인한 직원의 정보와 변경 요청"))
                .addTagsItem(new Tag().name("관리자 직원 관리").description("직원 계정 생성, 목록, 상세 및 퇴사 처리"))
                .addTagsItem(new Tag().name("관리자 인적사항 승인").description("직원 인적사항 변경 요청의 승인과 반려"))
                .addTagsItem(new Tag().name("관리자 Background Check").description("직원 배경 조회 실행과 결과 확인"))
                .addTagsItem(new Tag().name("상태 확인").description("애플리케이션 상태 확인"));
    }

    @Bean
    OpenApiCustomizer securityFilterEndpoints() {
        return openApi -> {
            ObjectSchema loginForm = new ObjectSchema();
            loginForm.addProperty("username", new StringSchema().description("로그인 아이디"));
            loginForm.addProperty("password", new PasswordSchema().description("비밀번호"));
            openApi.getPaths().addPathItem("/api/auth/login", new io.swagger.v3.oas.models.PathItem().post(
                    new io.swagger.v3.oas.models.Operation()
                            .tags(java.util.List.of("인증"))
                            .summary("로그인")
                            .description("application/x-www-form-urlencoded 형식으로 아이디와 비밀번호를 전송합니다. 성공하면 JSESSIONID 쿠키를 발급합니다.")
                            .requestBody(new RequestBody().required(true).content(new Content().addMediaType(
                                    MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                                    new io.swagger.v3.oas.models.media.MediaType().schema(loginForm))))
                            .responses(new ApiResponses()
                                    .addApiResponse("200", new ApiResponse().description("로그인 성공"))
                                    .addApiResponse("401", new ApiResponse().description("아이디 또는 비밀번호 불일치")))));
            openApi.getPaths().addPathItem("/api/auth/logout", new io.swagger.v3.oas.models.PathItem().post(
                    new io.swagger.v3.oas.models.Operation()
                            .tags(java.util.List.of("인증"))
                            .summary("로그아웃")
                            .description("현재 세션을 폐기합니다. CSRF 토큰 헤더가 필요합니다.")
                            .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("sessionCookie"))
                            .responses(new ApiResponses()
                                    .addApiResponse("204", new ApiResponse().description("로그아웃 완료"))
                                    .addApiResponse("401", new ApiResponse().description("로그인이 필요함")))));
        };
    }
}
