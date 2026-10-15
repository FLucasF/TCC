package com.loja.checkout.service.entrega;

import com.loja.checkout.enums.ModalidadeEntrega;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class CalculadoraFreteRegistry {

    private final Map<ModalidadeEntrega, CalculadoraFrete> calculadoras;

    public CalculadoraFreteRegistry(List<CalculadoraFrete> calculadorasDisponiveis) {
        this.calculadoras = new EnumMap<>(ModalidadeEntrega.class);
        for (CalculadoraFrete calculadora : calculadorasDisponiveis) {
            calculadoras.put(calculadora.getModalidade(), calculadora);
        }
    }

    public CalculadoraFrete obter(ModalidadeEntrega modalidade) {
        return calculadoras.get(modalidade);
    }
}
