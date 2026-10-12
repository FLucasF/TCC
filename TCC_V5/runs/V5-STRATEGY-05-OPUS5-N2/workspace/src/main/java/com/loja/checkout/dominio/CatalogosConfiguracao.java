package com.loja.checkout.dominio;

import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Reune as opcoes de cada tipo: uma opcao nova entra so criando a sua classe. */
@Configuration
public class CatalogosConfiguracao {

    @Bean
    Catalogo<ModalidadeEntrega> catalogoDeModalidades(List<ModalidadeEntrega> modalidades) {
        return new Catalogo<>(modalidades);
    }

    @Bean
    Catalogo<Cupom> catalogoDeCupons(List<Cupom> cupons) {
        return new Catalogo<>(cupons);
    }

    @Bean
    Catalogo<NivelClube> catalogoDeNiveis(List<NivelClube> niveis) {
        return new Catalogo<>(niveis);
    }

    @Bean
    Catalogo<FormaPagamento> catalogoDeFormasPagamento(List<FormaPagamento> formas) {
        return new Catalogo<>(formas);
    }
}
