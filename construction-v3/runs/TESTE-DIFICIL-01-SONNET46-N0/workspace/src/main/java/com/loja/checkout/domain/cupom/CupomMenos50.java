package com.loja.checkout.domain.cupom;

import com.loja.checkout.api.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CupomMenos50 implements AplicadorCupom {

    private static final BigDecimal MINIMO_SUBTOTAL = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public String getCodigo() {
        return "MENOS50";
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
        return subtotal.compareTo(MINIMO_SUBTOTAL) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {
        return DESCONTO;
    }
}
