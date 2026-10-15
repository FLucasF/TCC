package com.loja.checkout.calculo;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class Catalogos {

    @Bean
    Catalogo<ModalidadeEntrega> modalidades(List<ModalidadeEntrega> todas) {
        return new Catalogo<>(todas, ModalidadeEntrega::codigo);
    }

    @Bean
    Catalogo<Cupom> cupons(List<Cupom> todos) {
        return new Catalogo<>(todos, Cupom::codigo);
    }

    @Bean
    Catalogo<NivelClube> niveis(List<NivelClube> todos) {
        return new Catalogo<>(todos, NivelClube::codigo);
    }

    @Bean
    Catalogo<FormaPagamento> formasPagamento(List<FormaPagamento> todas) {
        return new Catalogo<>(todas, FormaPagamento::codigo);
    }
}
