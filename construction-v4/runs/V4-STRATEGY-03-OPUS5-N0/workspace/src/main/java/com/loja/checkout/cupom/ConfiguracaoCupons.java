package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Catalogo;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoCupons {

    @Bean
    public Catalogo<Cupom> catalogoCupons(List<Cupom> cupons) {
        return new Catalogo<>(cupons, Cupom::codigo);
    }
}
