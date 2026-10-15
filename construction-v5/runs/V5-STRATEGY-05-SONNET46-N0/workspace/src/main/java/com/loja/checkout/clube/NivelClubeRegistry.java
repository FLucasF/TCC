package com.loja.checkout.clube;

import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NivelClubeRegistry {

    private final Map<String, NivelClubeStrategy> strategies;

    public NivelClubeRegistry(List<NivelClubeStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(NivelClubeStrategy::getCodigo, Function.identity()));
    }

    public NivelClubeStrategy encontrar(String codigo) {
        if (codigo == null || !strategies.containsKey(codigo)) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        return strategies.get(codigo);
    }
}
