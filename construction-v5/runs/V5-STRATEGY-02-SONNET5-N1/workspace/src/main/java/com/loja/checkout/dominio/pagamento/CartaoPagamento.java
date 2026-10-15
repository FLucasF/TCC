package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.util.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class CartaoPagamento implements FormaPagamento {

    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(20);

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= PARCELAS_MINIMAS && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Dinheiro.arredondar(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
            return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalPedido, parcelas, valorParcela);
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS_MENSAL);
        BigDecimal fatorDesconto = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas, PRECISAO), PRECISAO));
        BigDecimal valorParcela = Dinheiro.arredondar(
                totalPedido.multiply(TAXA_JUROS_MENSAL).divide(fatorDesconto, PRECISAO));
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas)).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, parcelas, valorParcela);
    }
}
