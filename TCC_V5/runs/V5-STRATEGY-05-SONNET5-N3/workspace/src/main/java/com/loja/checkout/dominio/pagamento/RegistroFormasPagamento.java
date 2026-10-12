package com.loja.checkout.dominio.pagamento;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RegistroFormasPagamento {

    private final Map<String, CalculoPagamento> calculosPorCodigo;

    public RegistroFormasPagamento(List<CalculoPagamento> calculos) {
        this.calculosPorCodigo = calculos.stream()
                .collect(Collectors.toUnmodifiableMap(c -> c.forma().name(), Function.identity()));
    }

    public Optional<CalculoPagamento> buscar(String codigo) {
        return Optional.ofNullable(calculosPorCodigo.get(codigo));
    }
}
