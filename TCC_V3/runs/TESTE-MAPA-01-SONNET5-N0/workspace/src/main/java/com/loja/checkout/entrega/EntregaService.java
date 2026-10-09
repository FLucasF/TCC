package com.loja.checkout.entrega;

import com.loja.checkout.api.CodigoErro;
import com.loja.checkout.api.ErroPedidoException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EntregaService {

    private final Map<String, EntregaStrategy> estrategiasPorCodigo;

    public EntregaService(List<EntregaStrategy> estrategias) {
        this.estrategiasPorCodigo = estrategias.stream()
                .collect(Collectors.toMap(EntregaStrategy::codigo, Function.identity()));
    }

    public EntregaStrategy buscar(String codigo) {
        if (codigo == null || !estrategiasPorCodigo.containsKey(codigo)) {
            throw new ErroPedidoException(CodigoErro.MODALIDADE_INVALIDA);
        }
        return estrategiasPorCodigo.get(codigo);
    }

    public void validarDisponibilidade(EntregaStrategy entrega, BigDecimal pesoTotalKg, BigDecimal subtotalProdutos) {
        if (!entrega.disponivelPara(pesoTotalKg, subtotalProdutos)) {
            throw new ErroPedidoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
    }
}
