package com.loja.checkout.entrega;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.ErroCodigo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ModalidadeEntregaResolver {

    private final Map<String, ModalidadeEntrega> porCodigo;

    public ModalidadeEntregaResolver(List<ModalidadeEntrega> modalidades) {
        this.porCodigo = modalidades.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::getCodigo, Function.identity()));
    }

    public ModalidadeEntrega resolver(String codigo) {
        if (codigo == null || !porCodigo.containsKey(codigo)) {
            throw new CheckoutException(ErroCodigo.MODALIDADE_INVALIDA);
        }
        return porCodigo.get(codigo);
    }
}
