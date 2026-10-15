package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.loja.checkout.dto.ItemRequest;
import org.springframework.stereotype.Component;

@Component
public class BemVindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String getCodigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        return subtotal.multiply(PERCENTUAL).setScale(2, RoundingMode.HALF_EVEN);
    }
}
