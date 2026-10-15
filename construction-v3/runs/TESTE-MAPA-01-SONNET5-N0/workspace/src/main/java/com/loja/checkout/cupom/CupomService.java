package com.loja.checkout.cupom;

import com.loja.checkout.api.CodigoErro;
import com.loja.checkout.api.ErroPedidoException;
import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CupomService {

    private final Map<String, CupomStrategy> estrategiasPorCodigo;

    public CupomService(List<CupomStrategy> estrategias) {
        this.estrategiasPorCodigo = estrategias.stream()
                .collect(Collectors.toMap(CupomStrategy::codigo, Function.identity()));
    }

    public CupomStrategy buscar(String codigo) {
        if (!estrategiasPorCodigo.containsKey(codigo)) {
            throw new ErroPedidoException(CodigoErro.CUPOM_INVALIDO);
        }
        return estrategiasPorCodigo.get(codigo);
    }

    public void validarAplicavel(CupomStrategy cupom, List<Item> itens, BigDecimal subtotalProdutos) {
        if (!cupom.aplicavel(itens, subtotalProdutos)) {
            throw new ErroPedidoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
    }
}
