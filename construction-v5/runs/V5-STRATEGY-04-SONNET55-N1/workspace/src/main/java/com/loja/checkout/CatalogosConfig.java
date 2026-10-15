package com.loja.checkout;

import com.loja.checkout.clube.Nivel;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.Entrega;
import com.loja.checkout.pagamento.Pagamento;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogosConfig {
    @Bean
    Catalogo<Entrega> entregas(List<Entrega> opcoes) {
        return new Catalogo<>(opcoes);
    }

    @Bean
    Catalogo<Cupom> cupons(List<Cupom> opcoes) {
        return new Catalogo<>(opcoes);
    }

    @Bean
    Catalogo<Nivel> niveis(List<Nivel> opcoes) {
        return new Catalogo<>(opcoes);
    }

    @Bean
    Catalogo<Pagamento> pagamentos(List<Pagamento> opcoes) {
        return new Catalogo<>(opcoes);
    }
}
