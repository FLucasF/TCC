package com.loja.checkout.service.pagamento;

import com.loja.checkout.enums.FormaPagamento;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class CalculadoraPagamentoRegistry {

    private final Map<FormaPagamento, CalculadoraPagamento> calculadoras;

    public CalculadoraPagamentoRegistry(List<CalculadoraPagamento> calculadorasDisponiveis) {
        this.calculadoras = new EnumMap<>(FormaPagamento.class);
        for (CalculadoraPagamento calculadora : calculadorasDisponiveis) {
            calculadoras.put(calculadora.getForma(), calculadora);
        }
    }

    public CalculadoraPagamento obter(FormaPagamento forma) {
        return calculadoras.get(forma);
    }
}
