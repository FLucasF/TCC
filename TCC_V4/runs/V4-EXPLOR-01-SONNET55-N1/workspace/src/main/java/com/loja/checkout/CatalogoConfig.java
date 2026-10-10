package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.Entrega;
import com.loja.checkout.pagamento.FormaPagamento;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogoConfig {
    @Bean
    Catalogo<Entrega> entregas(List<Entrega> opcoes) {
        return new Catalogo<>(opcoes);
    }

    @Bean
    Catalogo<Cupom> cupons(List<Cupom> opcoes) {
        return new Catalogo<>(opcoes);
    }

    @Bean
    Catalogo<NivelClube> niveis(List<NivelClube> opcoes) {
        return new Catalogo<>(opcoes);
    }

    @Bean
    Catalogo<FormaPagamento> formasPagamento(List<FormaPagamento> opcoes) {
        return new Catalogo<>(opcoes);
    }
}
