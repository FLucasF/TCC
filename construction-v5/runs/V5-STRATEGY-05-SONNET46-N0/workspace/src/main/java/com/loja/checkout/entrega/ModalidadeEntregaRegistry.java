package com.loja.checkout.entrega;

import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ModalidadeEntregaRegistry {

    private final Map<String, ModalidadeEntregaStrategy> strategies;

    public ModalidadeEntregaRegistry(List<ModalidadeEntregaStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(ModalidadeEntregaStrategy::getCodigo, Function.identity()));
    }

    public ModalidadeEntregaStrategy encontrar(String codigo) {
        if (codigo == null || !strategies.containsKey(codigo)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        return strategies.get(codigo);
    }
}
