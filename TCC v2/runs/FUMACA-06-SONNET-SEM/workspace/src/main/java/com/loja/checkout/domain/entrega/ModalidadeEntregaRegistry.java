package com.loja.checkout.domain.entrega;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ModalidadeEntregaRegistry {

    private final Map<String, ModalidadeEntrega> modalidadesPorCodigo;

    public ModalidadeEntregaRegistry(List<ModalidadeEntrega> modalidades) {
        this.modalidadesPorCodigo = modalidades.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
    }

    public ModalidadeEntrega buscar(String codigo) {
        if (codigo == null || !modalidadesPorCodigo.containsKey(codigo)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
        return modalidadesPorCodigo.get(codigo);
    }
}
