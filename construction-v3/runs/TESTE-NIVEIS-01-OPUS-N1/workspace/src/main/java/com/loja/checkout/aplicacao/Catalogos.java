package com.loja.checkout.aplicacao;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Monta os catalogos com todas as opcoes que existem no sistema. */
@Configuration
class Catalogos {

    @Bean
    Catalogo<ModalidadeEntrega> catalogoModalidades(List<ModalidadeEntrega> modalidades) {
        return new Catalogo<>(modalidades);
    }

    @Bean
    Catalogo<Cupom> catalogoCupons(List<Cupom> cupons) {
        return new Catalogo<>(cupons);
    }

    @Bean
    Catalogo<NivelClube> catalogoNiveis(List<NivelClube> niveis) {
        return new Catalogo<>(niveis);
    }

    @Bean
    Catalogo<FormaPagamento> catalogoFormasPagamento(List<FormaPagamento> formas) {
        return new Catalogo<>(formas);
    }
}
