package com.loja.checkout.pagamento;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FormaPagamentoRegistry {

    private final Map<FormaPagamento, FormaPagamentoHandler> handlers;

    public FormaPagamentoRegistry(List<FormaPagamentoHandler> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(FormaPagamentoHandler::getFormaPagamento, Function.identity()));
    }

    public FormaPagamentoHandler resolver(String codigo) {
        FormaPagamento formaPagamento;
        try {
            formaPagamento = FormaPagamento.valueOf(codigo);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        FormaPagamentoHandler handler = handlers.get(formaPagamento);
        if (handler == null) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        return handler;
    }
}
