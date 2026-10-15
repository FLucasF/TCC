package com.loja;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public PedidoService pedidoService() {
        return new PedidoService();
    }
}
