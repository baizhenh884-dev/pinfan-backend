package com.pinfan.config;

import com.pinfan.interceptor.LoginInterceptor;
import com.pinfan.interceptor.RefreshTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private RefreshTokenInterceptor refreshTokenInterceptor;

    @Value("${pinfan.upload.dir}")
    private String uploadDir;

    @Value("${pinfan.upload.url-prefix}")
    private String uploadUrlPrefix;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(refreshTokenInterceptor)
                .addPathPatterns("/**")
                .order(0);

        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/test/ping",
                        "/doc.html",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/webjars/**",
                        "/favicon.ico",
                        "/error",
                        "/upload/**"
                )
                .order(1);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // URL 路径 /upload/xxx.png  →  磁盘文件 ${uploadDir}/xxx.png
        registry.addResourceHandler(uploadUrlPrefix + "**")
                .addResourceLocations("file:" + uploadDir);
    }
}
