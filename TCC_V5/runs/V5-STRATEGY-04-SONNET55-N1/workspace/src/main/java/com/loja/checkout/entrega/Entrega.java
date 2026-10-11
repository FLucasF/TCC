package com.loja.checkout.entrega;

import com.loja.checkout.Identificavel;
import java.math.BigDecimal;

/** Uma opção de entrega: quanto cobra, em quantos dias entrega e quais pedidos atende. */
public interface Entrega extends Identificavel {
    boolean atende(BigDecimal pesoKg);

    BigDecimal custo(BigDecimal pesoKg);

    int prazoDias();
}
