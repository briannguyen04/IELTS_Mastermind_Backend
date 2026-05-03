package com.ieltsmastermind.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static com.ieltsmastermind.common.constants.FileStorageConstants.*;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

//    <img src={practiceContent.thumbnailUrl} />
//    <audio controls src={practiceContent.audioUrl} />
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(IMAGE_PUBLIC_BASE_PATH + "**")
                .addResourceLocations("file:" + IMAGE_UPLOAD_DIR + "/");

        registry.addResourceHandler(THUMBNAIL_PUBLIC_BASE_PATH + "**")
                .addResourceLocations("file:" + THUMBNAIL_UPLOAD_DIR + "/");

        registry.addResourceHandler(AUDIO_PUBLIC_BASE_PATH + "**")
                .addResourceLocations("file:" + AUDIO_UPLOAD_DIR + "/");

        registry.addResourceHandler(AVATAR_PUBLIC_BASE_PATH + "**")
                .addResourceLocations("file:" + AVATAR_UPLOAD_DIR + "/");
    }
}
