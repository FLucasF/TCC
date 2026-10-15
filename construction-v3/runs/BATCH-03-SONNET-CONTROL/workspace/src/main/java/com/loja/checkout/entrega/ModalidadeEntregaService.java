package com.loja.checkout.entrega;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ModalidadeEntregaService {

    private final Map<String, ModalidadeEntrega> modalidadesPorCodigo;

    public ModalidadeEntregaService(List<ModalidadeEntrega> modalidades) {
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
