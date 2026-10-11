package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Cartão de crédito: até 3x sem juros, de 4x a 12x com juros de 1,99% ao mês (tabela Price). */
@Component
public class PagamentoCartao implements FormaPagamento {

    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Dinheiro.arredondar(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.CALCULO));
            return new ResultadoPagamento(Dinheiro.arredondar(totalPedido), parcelas, valorParcela);
        }

        BigDecimal valorParcela = parcelaPrice(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.arredondar(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcelas, valorParcela);
    }

    /** parcela = total × taxa ÷ (1 − (1 + taxa)^−parcelas) */
    private BigDecimal parcelaPrice(BigDecimal totalPedido, int parcelas) {
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal fatorDescontado = BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas), Dinheiro.CALCULO);
        BigDecimal divisor = BigDecimal.ONE.subtract(fatorDescontado);
        return Dinheiro.arredondar(
                totalPedido.multiply(TAXA_MENSAL).divide(divisor, Dinheiro.CALCULO));
    }
}
