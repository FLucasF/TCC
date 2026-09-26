package com.loja.checkout.pagamento;

import com.loja.checkout.domain.FormaPagamento;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CalculadoraPagamentoRegistry {

    private final Map<FormaPagamento, CalculadoraPagamento> porFormaPagamento;

    public CalculadoraPagamentoRegistry(List<CalculadoraPagamento> calculadoras) {
        this.porFormaPagamento = new EnumMap<>(FormaPagamento.class);
        calculadoras.forEach(c -> porFormaPagamento.put(c.formaPagamento(), c));
    }

    public Optional<CalculadoraPagamento> buscar(FormaPagamento formaPagamento) {
        return Optional.ofNullable(porFormaPagamento.get(formaPagamento));
    }
}
