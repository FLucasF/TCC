package com.loja.checkout.pagamento;

import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

@Component
public class CartaoDeCredito implements FormaPagamento {

    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int MAXIMO_PARCELAS = 12;
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(20);

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public boolean disponivelPara(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Dinheiro.arredondar(totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
            return new ResultadoPagamento(totalPedido, valorParcela);
        }
        BigDecimal fatorAcumulado = BigDecimal.ONE.add(TAXA_JUROS_MENSAL).pow(parcelas, PRECISAO);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fatorAcumulado, PRECISAO));
        BigDecimal valorParcela = Dinheiro.arredondar(totalPedido.multiply(TAXA_JUROS_MENSAL).divide(denominador, PRECISAO));
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        return new ResultadoPagamento(totalFinal, valorParcela);
    }
}
