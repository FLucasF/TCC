package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.AplicadorCupom;
import com.loja.checkout.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CupomLeve3Pague2 implements AplicadorCupom {
    private final List<ItemCarrinho> itens;

    public CupomLeve3Pague2(List<ItemCarrinho> itens) {
        this.itens = itens;
    }

    @Override
    public boolean podeAplicar(BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            int unidadesGratis = item.quantidade() / 3;
            BigDecimal descontoItem = item.precoUnitario()
                    .multiply(BigDecimal.valueOf(unidadesGratis));
            desconto = desconto.add(descontoItem);
        }

        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }
}
