package ru.server.access.config;


import com.google.gson.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;

@Configuration
public class GsonConfiguration {

    @Bean
    public Gson gson() {
        return new GsonBuilder().disableHtmlEscaping().serializeNulls().registerTypeAdapter(
                OffsetDateTime.class, new JsonSerializer<OffsetDateTime>() {
                            @Override
                            public JsonElement serialize(
                                    OffsetDateTime source,
                                    Type type,
                                    JsonSerializationContext context
                            ) {
                                return new JsonPrimitive(source.toString());
                            }
                        }
                ).create();
    }
}