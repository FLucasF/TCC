package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class PagamentoCartao implements FormaPagamento {

    private static final int MAX_PARCELAS_SEM_JUROS = 3;
    private static final int MAX_PARCELAS = 12;
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(20, RoundingMode.HALF_EVEN);

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas < 1 || parcelas > MAX_PARCELAS) {
            parcelamentoInvalido();
        }
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Dinheiro.arredondar(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
            return new ResultadoPagamento(totalPedido, valorParcela);
        }

        BigDecimal base = BigDecimal.ONE.add(TAXA_JUROS_MENSAL);
        BigDecimal basePotencia = base.pow(parcelas, PRECISAO);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(basePotencia, PRECISAO));
        BigDecimal valorParcelaBruto = totalPedido.multiply(TAXA_JUROS_MENSAL).divide(denominador, PRECISAO);
        BigDecimal valorParcela = Dinheiro.arredondar(valorParcelaBruto);
        BigDecimal totalFinal = Dinheiro.arredondar(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, valorParcela);
    }
}
