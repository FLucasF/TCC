package com.loja.checkout.domain.cupom;

import com.loja.checkout.utils.Arredondamento;
import java.math.BigDecimal;
import java.util.List;

public class CupomLeve3Pague2 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemCupom> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemCupom item : itens) {
            int unidadesDeGraca = item.quantidade() / 3;
            BigDecimal descontoItem = item.precoUnitario().multiply(new BigDecimal(unidadesDeGraca));
            desconto = desconto.add(descontoItem);
        }
        return Arredondamento.arredondarParaCentavos(desconto);
    }

    @Override
    public boolean podeAplicar(BigDecimal subtotal, List<ItemCupom> itens) {
        return true;
    }

    @Override
    public String obterCodigo() {
        return "LEVE3PAGUE2";
    }
}
