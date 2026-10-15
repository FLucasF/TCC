package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Cupom {

    boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens);

    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens, BigDecimal frete);

    Map<String, Cupom> REGISTRO = Map.of(
            "BEMVINDO10", new BemVindo10(),
            "MENOS50", new Menos50(),
            "FRETEGRATIS", new FreteGratis(),
            "LEVE3PAGUE2", new Leve3Pague2()
    );

    static Optional<Cupom> buscar(String codigo) {
        return Optional.ofNullable(REGISTRO.get(codigo));
    }
}
