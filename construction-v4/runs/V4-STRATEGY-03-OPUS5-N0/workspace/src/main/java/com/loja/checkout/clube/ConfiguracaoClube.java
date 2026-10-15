package com.loja.checkout.clube;

import com.loja.checkout.dominio.Catalogo;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoClube {

    @Bean
    public Catalogo<NivelClube> catalogoNiveis(List<NivelClube> niveis) {
        return new Catalogo<>(niveis, NivelClube::codigo);
    }
}
