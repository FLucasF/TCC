package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.FormaPagamento;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Coleta todas as implementações de {@link FormaPagamentoCalculadora} registradas como
 * componentes Spring e monta o mapa de lookup por forma de pagamento.
 */
@Component
public class PagamentoRegistry {

    private final Map<FormaPagamento, FormaPagamentoCalculadora> porForma;

    public PagamentoRegistry(List<FormaPagamentoCalculadora> formas) {
        this.porForma = formas.stream()
                .collect(Collectors.toUnmodifiableMap(FormaPagamentoCalculadora::getTipo, Function.identity()));
    }

    public FormaPagamentoCalculadora buscar(FormaPagamento forma) {
        return porForma.get(forma);
    }
}
