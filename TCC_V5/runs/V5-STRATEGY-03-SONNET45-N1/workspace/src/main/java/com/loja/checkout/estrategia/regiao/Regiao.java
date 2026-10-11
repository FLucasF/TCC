package com.loja.checkout.estrategia.regiao;

import java.math.BigDecimal;

public interface Regiao {
    BigDecimal calcularSeguro(BigDecimal valorProdutos);
}
