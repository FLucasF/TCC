package com.loja.checkout.pagamento;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

@Component
public class Cartao implements FormaPagamento {

    private static final int SEM_JUROS_ATE = 3;
    private static final int MAXIMO_PARCELAS = 12;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= SEM_JUROS_ATE) {
            return semJuros(totalPedido, parcelas);
        }
        return comJuros(totalPedido, parcelas);
    }

    /** Sem juros: o valor final é o próprio total; a parcela é o total dividido pelas vezes. */
    private ResultadoPagamento semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal parcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, Dinheiro.MODO);
        return new ResultadoPagamento(Dinheiro.centavos(totalPedido), parcela);
    }

    /** Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-n); final = parcela x n. */
    private ResultadoPagamento comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL64));
        BigDecimal parcela = totalPedido.multiply(TAXA_MENSAL)
                .divide(denominador, 2, Dinheiro.MODO);
        BigDecimal totalFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcela);
    }
}
