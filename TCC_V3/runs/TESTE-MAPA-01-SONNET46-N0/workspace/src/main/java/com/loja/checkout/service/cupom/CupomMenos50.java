package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemCarrinho;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
public class CupomMenos50 implements EstrategiaCupom {

    private static final BigDecimal MINIMO = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public String getCodigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete) {
        return subtotal.compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete) {
        return DESCONTO;
    }
}
