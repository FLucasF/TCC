package br.com.loja.pedidos.comum;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Configuracao {

    @Bean
    public Clock relogio() {
        return Clock.systemUTC();
    }
}
