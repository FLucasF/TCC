package com.loja.checkout.domain;

import com.loja.checkout.domain.clube.Clube;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RegistrosConfig {

    @Bean
    public RegistroPorCodigo<ModalidadeEntrega> registroModalidadeEntrega(List<ModalidadeEntrega> modalidades) {
        return new RegistroPorCodigo<>(modalidades);
    }

    @Bean
    public RegistroPorCodigo<Cupom> registroCupom(List<Cupom> cupons) {
        return new RegistroPorCodigo<>(cupons);
    }

    @Bean
    public RegistroPorCodigo<Clube> registroClube(List<Clube> clubes) {
        return new RegistroPorCodigo<>(clubes);
    }

    @Bean
    public RegistroPorCodigo<FormaPagamento> registroFormaPagamento(List<FormaPagamento> formasPagamento) {
        return new RegistroPorCodigo<>(formasPagamento);
    }
}
