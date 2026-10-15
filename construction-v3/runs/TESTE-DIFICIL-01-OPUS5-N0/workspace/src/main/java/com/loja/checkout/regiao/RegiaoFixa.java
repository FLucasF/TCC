package com.loja.checkout.regiao;

import java.math.BigDecimal;

/** Regiao com percentual de seguro fixo em tabela. */
record RegiaoFixa(String codigo, BigDecimal percentualSeguro) implements Regiao {
}
