package com.loja.checkout.clube;

import com.loja.checkout.api.CodigoErro;
import com.loja.checkout.api.ErroPedidoException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ClubeService {

    private final Map<String, ClubeStrategy> estrategiasPorCodigo;

    public ClubeService(List<ClubeStrategy> estrategias) {
        this.estrategiasPorCodigo = estrategias.stream()
                .collect(Collectors.toMap(ClubeStrategy::codigo, Function.identity()));
    }

    public ClubeStrategy buscar(String codigo) {
        if (codigo == null || !estrategiasPorCodigo.containsKey(codigo)) {
            throw new ErroPedidoException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
        return estrategiasPorCodigo.get(codigo);
    }
}
