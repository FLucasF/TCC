package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CatalogosConfig {

    @Bean
    Catalogo<ModalidadeEntrega> modalidades(List<ModalidadeEntrega> casos) {
        return new Catalogo<>(casos);
    }

    @Bean
    Catalogo<Cupom> cupons(List<Cupom> casos) {
        return new Catalogo<>(casos);
    }

    @Bean
    Catalogo<NivelClube> niveis(List<NivelClube> casos) {
        return new Catalogo<>(casos);
    }

    @Bean
    Catalogo<FormaPagamento> formasPagamento(List<FormaPagamento> casos) {
        return new Catalogo<>(casos);
    }
}
