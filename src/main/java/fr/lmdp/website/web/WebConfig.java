package fr.lmdp.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Expose le dossier src/main/resources/images (logos, planche graphique)
 * sous l'URL /images/** sans avoir à déplacer les fichiers existants,
 * ainsi que le dossier (hors classpath) des photos de produits uploadées
 * depuis le back-office, sous /uploads/**.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cawl.uploads-dir:./data/uploads/products}")
    private String uploadsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/images/");

        String location = uploadsDir.endsWith("/") ? uploadsDir : uploadsDir + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + location);
    }
}



