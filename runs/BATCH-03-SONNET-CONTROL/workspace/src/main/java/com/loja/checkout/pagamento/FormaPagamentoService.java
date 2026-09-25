package com.loja.checkout.pagamento;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FormaPagamentoService {

    private final Map<String, FormaPagamento> formasPorCodigo;

    public FormaPagamentoService(List<FormaPagamento> formas) {
        this.formasPorCodigo = formas.stream()
                .collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
    }

    public FormaPagamento buscar(String codigo) {
        if (codigo == null || !formasPorCodigo.containsKey(codigo)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        return formasPorCodigo.get(codigo);
    }
}
