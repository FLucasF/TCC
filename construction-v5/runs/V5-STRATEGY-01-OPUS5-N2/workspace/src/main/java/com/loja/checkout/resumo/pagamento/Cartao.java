package com.loja.checkout.resumo.pagamento;

import com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;

/** Em até 3x sem juros; de 4x a 12x com juros de 1,99% ao mês pela tabela Price. */
public class Cartao implements FormaPagamento {

    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public Parcelamento calcular(BigDecimal totalPedido, int parcelas) {
        return parcelas <= PARCELAS_SEM_JUROS
                ? semJuros(totalPedido, parcelas)
                : comJuros(totalPedido, parcelas);
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    private Parcelamento semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorParcela = Dinheiro.centavos(
                totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.CALCULO));
        return new Parcelamento(Dinheiro.centavos(totalPedido), valorParcela);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
    private Parcelamento comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Dinheiro.CALCULO);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, Dinheiro.CALCULO));
        BigDecimal valorParcela = Dinheiro.centavos(
                totalPedido.multiply(TAXA_MENSAL).divide(divisor, Dinheiro.CALCULO));
        BigDecimal totalFinal = Dinheiro.centavos(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        return new Parcelamento(totalFinal, valorParcela);
    }
}
