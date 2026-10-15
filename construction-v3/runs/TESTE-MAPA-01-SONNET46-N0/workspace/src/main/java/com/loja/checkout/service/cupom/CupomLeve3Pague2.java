package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemCarrinho;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CupomLeve3Pague2 implements EstrategiaCupom {

    @Override
    public String getCodigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete) {
        // para cada item: floor(quantidade / 3) × precoUnitario
        BigDecimal total = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            int gratis = item.getQuantidade() / 3;
            if (gratis > 0) {
                total = total.add(item.getPrecoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
        }
        return total.setScale(2, RoundingMode.HALF_EVEN);
    }
}
