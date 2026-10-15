package com.loja.checkout.cupom;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CupomRegistry {

    private final Map<String, CupomHandler> handlers;

    public CupomRegistry(List<CupomHandler> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(CupomHandler::getCodigo, Function.identity()));
    }

    public CupomHandler resolver(String codigo) {
        CupomHandler handler = handlers.get(codigo);
        if (handler == null) {
            throw new CheckoutException(CodigoErro.CUPOM_INVALIDO);
        }
        return handler;
    }
}
