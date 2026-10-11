package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

public final class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA = new BigDecimal("0.0199");
    private static final int SEM_JUROS_ATE = 3;
    private static final int MAX_PARCELAS = 12;

    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    public boolean atende(BigDecimal totalPedido, int parcelas) { return true; }

    public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= SEM_JUROS_ATE) {
            BigDecimal parcela = Dinheiro.arredondar(
                totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.MC));
            return new Pagamento(Dinheiro.arredondar(totalPedido), parcela);
        }
        BigDecimal um = BigDecimal.ONE;
        BigDecimal potencia = um.add(TAXA).pow(parcelas);
        BigDecimal denom = potencia.subtract(um).divide(potencia, Dinheiro.MC);
        BigDecimal parcela = Dinheiro.arredondar(
            totalPedido.multiply(TAXA).divide(denom, Dinheiro.MC));
        BigDecimal totalFinal = Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new Pagamento(totalFinal, parcela);
    }
}
