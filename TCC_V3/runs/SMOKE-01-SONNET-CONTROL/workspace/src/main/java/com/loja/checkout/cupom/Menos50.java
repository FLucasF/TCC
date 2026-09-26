package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component("MENOS50")
public class Menos50 implements Cupom {

    private static final BigDecimal VALOR_DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal SUBTOTAL_MINIMO = new BigDecimal("300.00");

    @Override
    public void validarAplicabilidade(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
        if (subtotalProdutos.compareTo(SUBTOTAL_MINIMO) < 0) {
            naoAplicavel();
        }
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return VALOR_DESCONTO;
    }
}
