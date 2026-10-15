package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.Erro;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Reúne as opções de cada tipo que a loja oferece hoje. */
@Configuration
public class Catalogos {

    @Bean
    public Catalogo<ModalidadeEntrega> modalidades(List<ModalidadeEntrega> modalidades) {
        return new Catalogo<>(modalidades, Erro.MODALIDADE_INVALIDA);
    }

    @Bean
    public Catalogo<Cupom> cupons(List<Cupom> cupons) {
        return new Catalogo<>(cupons, Erro.CUPOM_INVALIDO);
    }

    @Bean
    public Catalogo<NivelClube> niveisClube(List<NivelClube> niveis) {
        return new Catalogo<>(niveis, Erro.NIVEL_CLUBE_INVALIDO);
    }

    @Bean
    public Catalogo<FormaPagamento> formasPagamento(List<FormaPagamento> formas) {
        return new Catalogo<>(formas, Erro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Bean
    public Catalogo<Regiao> regioes() {
        return new Catalogo<>(List.of(Regiao.values()), Erro.REGIAO_INVALIDA);
    }
}
