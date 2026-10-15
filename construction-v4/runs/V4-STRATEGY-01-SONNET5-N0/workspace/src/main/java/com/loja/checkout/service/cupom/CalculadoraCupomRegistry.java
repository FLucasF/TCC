package com.loja.checkout.service.cupom;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CalculadoraCupomRegistry {

    private final Map<String, CalculadoraCupom> calculadoras;

    public CalculadoraCupomRegistry(List<CalculadoraCupom> calculadorasDisponiveis) {
        this.calculadoras = calculadorasDisponiveis.stream()
                .collect(Collectors.toMap(CalculadoraCupom::getCodigo, c -> c));
    }

    public CalculadoraCupom obter(String codigo) {
        return calculadoras.get(codigo);
    }
}
