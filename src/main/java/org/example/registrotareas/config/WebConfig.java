package org.example.registrotareas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Sirve las imágenes guardadas en la carpeta "uploads/" como recursos estáticos.
 * URL pública: {dominio}/api/uploads/tareas/xxx.jpg
 * (el context-path /api se antepone automáticamente).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.uploads-dir:uploads}")
    private String uploadsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absoluta = Paths.get(uploadsDir).toAbsolutePath().normalize().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absoluta + "/");
    }
}
