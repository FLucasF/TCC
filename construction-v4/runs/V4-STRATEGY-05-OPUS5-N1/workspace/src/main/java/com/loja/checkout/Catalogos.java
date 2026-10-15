package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Cada catalogo reune as implementacoes existentes de uma opcao. */
@Configuration
public class Catalogos {

    @Bean
    public Catalogo<ModalidadeEntrega> modalidadesEntrega(List<ModalidadeEntrega> modalidades) {
        return new Catalogo<>(modalidades);
    }

    @Bean
    public Catalogo<Cupom> cupons(List<Cupom> cupons) {
        return new Catalogo<>(cupons);
    }

    @Bean
    public Catalogo<NivelClube> niveisClube(List<NivelClube> niveis) {
        return new Catalogo<>(niveis);
    }

    @Bean
    public Catalogo<FormaPagamento> formasPagamento(List<FormaPagamento> formas) {
        return new Catalogo<>(formas);
    }
}
