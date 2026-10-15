package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Junta os casos de cada eixo num catálogo. Caso novo entra só por ser bean. */
@Configuration
class CatalogosConfig {

    @Bean
    Catalogo<ModalidadeEntrega> catalogoEntrega(List<ModalidadeEntrega> modalidades) {
        return new Catalogo<>(modalidades);
    }

    @Bean
    Catalogo<Cupom> catalogoCupons(List<Cupom> cupons) {
        return new Catalogo<>(cupons);
    }

    @Bean
    Catalogo<NivelClube> catalogoClube(List<NivelClube> niveis) {
        return new Catalogo<>(niveis);
    }

    @Bean
    Catalogo<FormaPagamento> catalogoPagamento(List<FormaPagamento> formas) {
        return new Catalogo<>(formas);
    }
}
