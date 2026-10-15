package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA_MES = new BigDecimal("0.0199");
    private static final MathContext MC = new MathContext(20);
    private static final int LIMITE_SEM_JUROS = 3;

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Ajuste calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= LIMITE_SEM_JUROS) {
            BigDecimal totalFinal = Dinheiro.arredonda(totalPedido);
            BigDecimal parcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            BigDecimal ajuste = Dinheiro.arredonda(totalFinal.subtract(totalPedido));
            return new Ajuste(totalFinal, parcela, ajuste);
        }
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MES).pow(parcelas, MC);
        BigDecimal numerador = totalPedido.multiply(TAXA_MES).multiply(fator);
        BigDecimal denominador = fator.subtract(BigDecimal.ONE);
        BigDecimal parcela = numerador.divide(denominador, 2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = Dinheiro.arredonda(parcela.multiply(BigDecimal.valueOf(parcelas)));
        BigDecimal ajuste = Dinheiro.arredonda(totalFinal.subtract(totalPedido));
        return new Ajuste(totalFinal, parcela, ajuste);
    }
}
