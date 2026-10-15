package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Catalogo;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoEntregas {

    @Bean
    public Catalogo<ModalidadeEntrega> catalogoModalidades(List<ModalidadeEntrega> modalidades) {
        return new Catalogo<>(modalidades, ModalidadeEntrega::codigo);
    }
}
