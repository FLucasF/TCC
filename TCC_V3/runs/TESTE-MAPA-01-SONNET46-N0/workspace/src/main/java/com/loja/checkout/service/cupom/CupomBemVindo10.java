package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemCarrinho;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CupomBemVindo10 implements EstrategiaCupom {

    @Override
    public String getCodigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete) {
        return subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_EVEN);
    }
}
