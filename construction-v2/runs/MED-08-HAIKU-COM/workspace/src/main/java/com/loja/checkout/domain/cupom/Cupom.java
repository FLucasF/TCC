package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemCupom> itens);
    boolean podeAplicar(BigDecimal subtotal, List<ItemCupom> itens);
    String obterCodigo();
}
