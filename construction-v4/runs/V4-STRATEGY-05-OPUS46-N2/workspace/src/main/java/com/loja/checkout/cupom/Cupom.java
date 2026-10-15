package com.loja.checkout.cupom;

import com.loja.checkout.CheckoutRequest.ItemRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface Cupom {

    BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete);

    boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens);

    Map<String, Cupom> REGISTRO = Map.of(
            "BEMVINDO10", new Bemvindo10(),
            "MENOS50", new Menos50(),
            "FRETEGRATIS", new FreteGratis(),
            "LEVE3PAGUE2", new Leve3Pague2()
    );

    static Cupom buscar(String codigo) {
        return codigo == null ? null : REGISTRO.get(codigo);
    }
}
