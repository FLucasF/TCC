package com.loja.checkout.delivery;

import com.loja.checkout.PedidoContext;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.ErroCodigo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeliveryService {

    private final Map<String, ModalidadeEntregaStrategy> estrategiasPorCodigo;

    public DeliveryService(List<ModalidadeEntregaStrategy> estrategias) {
        this.estrategiasPorCodigo = estrategias.stream()
                .collect(Collectors.toMap(ModalidadeEntregaStrategy::getCodigo, Function.identity()));
    }

    public ModalidadeEntregaStrategy resolver(String codigo, PedidoContext pedido) {
        if (codigo == null || codigo.isBlank()) {
            throw new CheckoutException(ErroCodigo.MODALIDADE_INVALIDA);
        }
        ModalidadeEntregaStrategy estrategia = estrategiasPorCodigo.get(codigo);
        if (estrategia == null) {
            throw new CheckoutException(ErroCodigo.MODALIDADE_INVALIDA);
        }
        if (!estrategia.disponivelPara(pedido)) {
            throw new CheckoutException(ErroCodigo.MODALIDADE_INDISPONIVEL);
        }
        return estrategia;
    }
}
