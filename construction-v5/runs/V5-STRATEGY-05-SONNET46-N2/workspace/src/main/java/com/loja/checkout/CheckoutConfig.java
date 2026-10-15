package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class CheckoutConfig {

    @Bean
    Map<String, ModalidadeEntrega> entregas(List<ModalidadeEntrega> todas) {
        return todas.stream().collect(Collectors.toMap(ModalidadeEntrega::codigo, e -> e));
    }

    @Bean
    Map<String, Cupom> cupons(List<Cupom> todos) {
        return todos.stream().collect(Collectors.toMap(Cupom::codigo, c -> c));
    }

    @Bean
    Map<String, NivelClube> niveis(List<NivelClube> todos) {
        return todos.stream().collect(Collectors.toMap(NivelClube::codigo, n -> n));
    }

    @Bean
    Map<String, FormaPagamento> pagamentos(List<FormaPagamento> todos) {
        return todos.stream().collect(Collectors.toMap(FormaPagamento::codigo, p -> p));
    }
}
