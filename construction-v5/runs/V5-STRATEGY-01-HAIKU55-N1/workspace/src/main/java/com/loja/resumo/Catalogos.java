package com.loja.resumo;

import com.loja.resumo.clube.NivelClube;
import com.loja.resumo.cupom.Cupom;
import com.loja.resumo.entrega.OpcaoEntrega;
import com.loja.resumo.pagamento.FormaPagamento;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class Catalogos {

    @Bean
    Catalogo<OpcaoEntrega> entregas(List<OpcaoEntrega> opcoes) {
        return new Catalogo<>(opcoes);
    }

    @Bean
    Catalogo<Cupom> cupons(List<Cupom> cupons) {
        return new Catalogo<>(cupons);
    }

    @Bean
    Catalogo<FormaPagamento> formasPagamento(List<FormaPagamento> formas) {
        return new Catalogo<>(formas);
    }

    @Bean
    Catalogo<NivelClube> niveis(List<NivelClube> niveis) {
        return new Catalogo<>(niveis);
    }

    @Bean
    Catalogo<Regiao> regioes() {
        return new Catalogo<>(List.of(Regiao.values()));
    }
}
