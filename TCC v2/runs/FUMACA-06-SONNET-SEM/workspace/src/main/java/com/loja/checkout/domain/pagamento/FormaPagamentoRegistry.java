package com.loja.checkout.domain.pagamento;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FormaPagamentoRegistry {

    private final Map<String, FormaPagamento> formasPorCodigo;

    public FormaPagamentoRegistry(List<FormaPagamento> formasPagamento) {
        this.formasPorCodigo = formasPagamento.stream()
                .collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
    }

    public FormaPagamento buscar(String codigo) {
        if (codigo == null || !formasPorCodigo.containsKey(codigo)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        return formasPorCodigo.get(codigo);
    }
}
