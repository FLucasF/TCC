package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Monta cada catálogo com os casos existentes. Um caso novo só precisa ser um
 * bean da sua interface: ninguém aqui muda.
 */
@Configuration
class Catalogos {

    @Bean
    Catalogo<ModalidadeEntrega> entregas(List<ModalidadeEntrega> casos) {
        return new Catalogo<>(casos, CodigoErro.MODALIDADE_INVALIDA);
    }

    @Bean
    Catalogo<Cupom> cupons(List<Cupom> casos) {
        return new Catalogo<>(casos, CodigoErro.CUPOM_INVALIDO);
    }

    @Bean
    Catalogo<NivelClube> niveisClube(List<NivelClube> casos) {
        return new Catalogo<>(casos, CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Bean
    Catalogo<FormaPagamento> formasPagamento(List<FormaPagamento> casos) {
        return new Catalogo<>(casos, CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }
}
