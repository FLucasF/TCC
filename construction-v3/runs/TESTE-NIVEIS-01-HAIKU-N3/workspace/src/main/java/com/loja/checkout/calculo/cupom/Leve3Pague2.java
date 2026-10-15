package com.loja.checkout.calculo.cupom;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2 implements CupomCalculador {
    private List<ItemCarrinho> itens;

    public Leve3Pague2(List<ItemCarrinho> itens) {
        this.itens = itens;
    }

    @Override
    public boolean ehAplicavel(BigDecimal subtotal) {
        return true;
    }

    @Override
    public BigDecimal calcular(BigDecimal subtotal, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            int unidades = item.quantidade;
            int gratuitas = unidades / 3;
            BigDecimal descontoItem = BigDecimal.valueOf(gratuitas)
                    .multiply(Arredondador.arredondar(item.precoUnitario));
            desconto = desconto.add(descontoItem);
        }
        return Arredondador.arredondar(desconto);
    }
}
