package com.loja.checkout.cupom;

import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.Pedido;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Leve3Pague2 implements Cupom {
    private static final String CODIGO = "LEVE3PAGUE2";

    @Override
    public String getCodigo() {
        return CODIGO;
    }

    @Override
    public boolean podeAplicar(Pedido pedido, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(Pedido pedido, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (Item item : pedido.getItens()) {
            int quantidade = item.getQuantidade();
            int unidadesGratis = quantidade / 3;

            if (unidadesGratis > 0) {
                BigDecimal descontoItem = item.getPrecoUnitario()
                        .multiply(BigDecimal.valueOf(unidadesGratis))
                        .setScale(2, RoundingMode.HALF_EVEN);
                desconto = desconto.add(descontoItem);
            }
        }

        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }
}
