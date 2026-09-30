package com.loja.checkout.estrategia.cupom;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;
import java.util.List;

public class CupomLeve3Pague2 implements EstrategiaCupom {
    private final List<BigDecimal> precos;

    public CupomLeve3Pague2(List<BigDecimal> precos) {
        this.precos = precos;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Integer> quantidades) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (int i = 0; i < quantidades.size(); i++) {
            int quantidade = quantidades.get(i);
            int unitaisFree = quantidade / 3;
            BigDecimal descontoItem = precos.get(i).multiply(new BigDecimal(unitaisFree));
            desconto = desconto.add(descontoItem);
        }
        return Arredondamento.arredondarMeioParaPar(desconto);
    }

    @Override
    public void validar(BigDecimal subtotalProdutos, List<Integer> quantidades) throws IllegalArgumentException {
    }
}
