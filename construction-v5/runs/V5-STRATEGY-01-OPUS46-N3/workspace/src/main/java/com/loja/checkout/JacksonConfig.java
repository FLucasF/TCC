package com.loja.checkout;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer bigDecimalPlain() {
        return builder -> builder.enable(tools.jackson.core.StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN);
    }
}
