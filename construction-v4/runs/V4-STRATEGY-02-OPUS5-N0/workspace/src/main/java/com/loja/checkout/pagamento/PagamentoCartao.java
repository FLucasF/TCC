package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

/** Cartão de crédito: até 3x sem juros, de 4x a 12x com juros de 1,99% ao mês (tabela Price). */
@Component
public class PagamentoCartao implements FormaPagamento {

    private static final int MAXIMO_PARCELAS = 12;
    private static final int MAXIMO_PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(20);

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAXIMO_PARCELAS_SEM_JUROS) {
            BigDecimal totalFinal = Dinheiro.arredondar(totalPedido);
            BigDecimal valorParcela = Dinheiro.arredondar(
                    totalFinal.divide(BigDecimal.valueOf(parcelas), PRECISAO));
            return new ResultadoPagamento(totalFinal, valorParcela);
        }
        BigDecimal valorParcela = Dinheiro.arredondar(parcelaPrice(totalPedido, parcelas));
        BigDecimal totalFinal = Dinheiro.arredondar(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, valorParcela);
    }

    /** parcela = total × taxa ÷ (1 − (1 + taxa)^−parcelas) */
    private BigDecimal parcelaPrice(BigDecimal total, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, PRECISAO);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO));
        return total.multiply(TAXA_MENSAL).divide(divisor, PRECISAO);
    }
}
