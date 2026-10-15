package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.erro.CheckoutException;
import com.loja.checkout.domain.erro.ErroCheckout;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Acha a modalidade pelo codigo sem cadeia de ifs; nao achou -> MODALIDADE_INVALIDA. */
@Component
public class EntregaResolver {

    private final Map<String, ModalidadeEntrega> porCodigo;

    public EntregaResolver(List<ModalidadeEntrega> modalidades) {
        this.porCodigo = modalidades.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
    }

    public ModalidadeEntrega resolver(String codigo) {
        ModalidadeEntrega modalidade = codigo == null ? null : porCodigo.get(codigo);
        if (modalidade == null) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INVALIDA);
        }
        return modalidade;
    }
}
