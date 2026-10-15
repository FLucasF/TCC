package com.loja.checkout.pagamento;

import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.dominio.FormaPagamento;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

@Component
public class CartaoPagamento implements Pagamento {

    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(20);

    @Override
    public FormaPagamento forma() {
        return FormaPagamento.CARTAO;
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
            BigDecimal valorParcela = Dinheiro.arredondar(totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
            return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalPedido, valorParcela);
        }

        BigDecimal fatorAcumulado = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, PRECISAO);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fatorAcumulado, PRECISAO));
        BigDecimal valorParcela = Dinheiro.arredondar(
                totalPedido.multiply(TAXA_MENSAL).divide(denominador, PRECISAO));
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }
}
