package com.loja.checkout.clube;

import com.loja.checkout.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public interface BeneficioClube {
    BigDecimal calcularCredito(List<ItemCarrinho> itens);
    boolean freteGratis();
    boolean ganharBrinde(BigDecimal subtotalProdutos);
}
