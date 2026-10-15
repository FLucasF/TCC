package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.comum.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

@Component
public class Cartao implements FormaPagamento {

    private static final int SEM_JUROS_ATE = 3;
    private static final int MAX_PARCELAS = 12;
    private static final BigDecimal TAXA = new BigDecimal("0.0199");
    private static final MathContext MC = MathContext.DECIMAL64;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        return parcelas <= SEM_JUROS_ATE
                ? semJuros(totalPedido, parcelas)
                : comJuros(totalPedido, parcelas);
    }

    private ResultadoPagamento semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorFinal = Dinheiro.centavos(totalPedido);
        BigDecimal parcela = Dinheiro.centavos(
                totalPedido.divide(BigDecimal.valueOf(parcelas), MC));
        return new ResultadoPagamento(valorFinal, parcela);
    }

    private ResultadoPagamento comJuros(BigDecimal totalPedido, int parcelas) {
        // Tabela Price: parcela = total * taxa / (1 - (1 + taxa)^-n)
        BigDecimal fator = BigDecimal.ONE.add(TAXA).pow(parcelas);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MC));
        BigDecimal parcela = Dinheiro.centavos(
                totalPedido.multiply(TAXA).divide(divisor, MC));
        BigDecimal valorFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(valorFinal, parcela);
    }
}
