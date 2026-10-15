package com.loja.checkout.config;

import com.loja.checkout.domain.clube.*;
import com.loja.checkout.domain.cupom.*;
import com.loja.checkout.domain.entrega.*;
import com.loja.checkout.domain.pagamento.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class DomainConfig {

    @Bean
    public Map<String, ModalidadeEntrega> modalidades() {
        return Map.of(
                "ECONOMICA", new Economica(),
                "EXPRESSA", new Expressa(),
                "RETIRADA_LOJA", new RetiradaLoja(),
                "MOTOBOY", new Motoboy());
    }

    @Bean
    public Map<String, Cupom> cupons() {
        return Map.of(
                "BEMVINDO10", new BemVindo10(),
                "MENOS50", new Menos50(),
                "FRETEGRATIS", new FreteGratis(),
                "LEVE3PAGUE2", new Leve3Pague2());
    }

    @Bean
    public Map<String, NivelClube> niveisClube() {
        return Map.of(
                "BRONZE", new Bronze(),
                "PRATA", new Prata(),
                "OURO", new Ouro());
    }

    @Bean
    public Map<String, FormaPagamento> formasPagamento() {
        return Map.of(
                "PIX", new Pix(),
                "CARTAO", new Cartao(),
                "BOLETO", new Boleto());
    }
}
