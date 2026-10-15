package com.loja.checkout.entrega;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ModalidadeEntregaRegistry {

    private final Map<ModalidadeEntrega, ModalidadeEntregaHandler> handlers;

    public ModalidadeEntregaRegistry(List<ModalidadeEntregaHandler> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(ModalidadeEntregaHandler::getModalidade, Function.identity()));
    }

    public ModalidadeEntregaHandler resolver(String codigo) {
        ModalidadeEntrega modalidade;
        try {
            modalidade = ModalidadeEntrega.valueOf(codigo);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
        ModalidadeEntregaHandler handler = handlers.get(modalidade);
        if (handler == null) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
        return handler;
    }
}
