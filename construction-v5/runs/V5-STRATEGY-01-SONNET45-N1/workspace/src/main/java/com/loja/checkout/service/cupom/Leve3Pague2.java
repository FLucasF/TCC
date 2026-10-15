package com.loja.checkout.service.cupom;

import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class Leve3Pague2 implements Cupom {
    @Override
    public String getCodigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(Pedido pedido, BigDecimal subtotalProdutos, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (Item item : pedido.itens()) {
            int unidadesGratis = item.quantidade() / 3;
            BigDecimal descontoItem = item.precoUnitario()
                .multiply(BigDecimal.valueOf(unidadesGratis));
            desconto = desconto.add(descontoItem);
        }

        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }
}
