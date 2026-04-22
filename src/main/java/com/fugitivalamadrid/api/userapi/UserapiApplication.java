package com.fugitivalamadrid.api.userapi;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableCaching
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class UserapiApplication {

    public static void main(String[] args) {
        // Load .env variables
        Dotenv dotenv = Dotenv.configure()
                .filename(".env.local")       // reads .env.local locally
                .ignoreIfMissing()            // ignored in Docker (file won't exist)
                .load();

        dotenv.entries().forEach(e ->
                System.setProperty(e.getKey(), e.getValue())
        );

        SpringApplication.run(UserapiApplication.class, args);
    }

}
