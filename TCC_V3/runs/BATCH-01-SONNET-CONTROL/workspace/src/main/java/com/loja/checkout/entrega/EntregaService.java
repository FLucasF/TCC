package com.loja.checkout.entrega;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EntregaService {

    private final Map<String, ModalidadeEntregaEstrategia> estrategiasPorCodigo;

    public EntregaService(List<ModalidadeEntregaEstrategia> estrategias) {
        this.estrategiasPorCodigo = estrategias.stream()
                .collect(Collectors.toMap(ModalidadeEntregaEstrategia::getCodigo, Function.identity()));
    }

    public ModalidadeEntregaEstrategia buscarEstrategia(String codigo) {
        ModalidadeEntregaEstrategia estrategia = codigo == null ? null : estrategiasPorCodigo.get(codigo);
        if (estrategia == null) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
        return estrategia;
    }
}
