package com.loja.resumo.entrega;

import com.loja.resumo.Codigado;
import java.math.BigDecimal;

public interface OpcaoEntrega extends Codigado {

    BigDecimal frete(BigDecimal pesoKg);

    int prazoDias();

    default boolean atende(BigDecimal pesoKg) {
        return true;
    }
}
