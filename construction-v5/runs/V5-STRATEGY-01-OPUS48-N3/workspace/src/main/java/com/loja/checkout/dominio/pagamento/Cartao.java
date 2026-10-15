package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cartão de crédito: de 1 a 12 parcelas. Até 3x sem juros (o valor final é o
 * próprio total e a parcela é o total dividido pelas parcelas). De 4x a 12x com
 * juros de 1,99% ao mês pela tabela Price; o valor final é a parcela vezes o
 * número de parcelas.
 */
@Component
public class Cartao implements FormaPagamento {

    private static final int SEM_JUROS_ATE = 3;
    private static final int MAXIMO_PARCELAS = 12;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int ESCALA_INTERMEDIARIA = 10;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= SEM_JUROS_ATE) {
            BigDecimal parcela = Dinheiro.centavos(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), ESCALA_INTERMEDIARIA, RoundingMode.HALF_EVEN));
            return new ResultadoPagamento(totalPedido, parcela);
        }
        // Tabela Price, toda em BigDecimal para ser determinística:
        // parcela = total × taxa × (1+taxa)^n / ((1+taxa)^n − 1).
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal numerador = totalPedido.multiply(TAXA_MENSAL).multiply(fator);
        BigDecimal denominador = fator.subtract(BigDecimal.ONE);
        BigDecimal parcela = Dinheiro.centavos(
                numerador.divide(denominador, ESCALA_INTERMEDIARIA, RoundingMode.HALF_EVEN));
        BigDecimal totalFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcela);
    }
}
