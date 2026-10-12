package com.loja.checkout.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

import java.math.BigDecimal;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public JsonMapper jsonMapper() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(BigDecimal.class, new StdSerializer<>(BigDecimal.class) {
            @Override
            public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext ctx)
                    throws JacksonException {
                gen.writeNumber(value.toPlainString());
            }
        });
        return JsonMapper.builder().addModule(module).build();
    }
}
