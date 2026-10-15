package com.loja.checkout.dominio;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Monta os catalogos com as opcoes que existem hoje de cada tipo. */
@Configuration
public class Catalogos {

    @Bean
    public Catalogo<ModalidadeEntrega> catalogoDeEntrega(List<ModalidadeEntrega> modalidades) {
        return new Catalogo<>(ErroCheckout.MODALIDADE_INVALIDA, modalidades);
    }

    @Bean
    public Catalogo<Cupom> catalogoDeCupons(List<Cupom> cupons) {
        return new Catalogo<>(ErroCheckout.CUPOM_INVALIDO, cupons);
    }

    @Bean
    public Catalogo<NivelClube> catalogoDoClube(List<NivelClube> niveis) {
        return new Catalogo<>(ErroCheckout.NIVEL_CLUBE_INVALIDO, niveis);
    }

    @Bean
    public Catalogo<FormaPagamento> catalogoDePagamento(List<FormaPagamento> formas) {
        return new Catalogo<>(ErroCheckout.FORMA_PAGAMENTO_INVALIDA, formas);
    }

    @Bean
    public Catalogo<Regiao> catalogoDeRegioes() {
        return new Catalogo<>(ErroCheckout.REGIAO_INVALIDA, List.of(Regiao.values()));
    }
}
