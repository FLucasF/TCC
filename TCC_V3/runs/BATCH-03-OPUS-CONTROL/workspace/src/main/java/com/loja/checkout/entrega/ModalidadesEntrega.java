package com.loja.checkout.entrega;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ErroCheckout;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Catalogo das opcoes de entrega disponiveis no sistema. */
@Component
public class ModalidadesEntrega {

    private final Map<String, ModalidadeEntrega> porCodigo;

    public ModalidadesEntrega(List<ModalidadeEntrega> modalidades) {
        this.porCodigo = modalidades.stream().collect(Collectors.toMap(
                ModalidadeEntrega::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public ModalidadeEntrega buscar(String codigo) {
        ModalidadeEntrega modalidade = codigo == null ? null : porCodigo.get(codigo);
        if (modalidade == null) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INVALIDA);
        }
        return modalidade;
    }
}
