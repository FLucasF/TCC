package com.loja.checkout.pagamento;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * CARTAO: de 1x a 12x. Sem juros ate 3x (ou ate 6x para quem o clube permite);
 * acima disso, juros de 1,99% ao mes pela tabela Price.
 */
@Component
public class Cartao implements FormaPagamento {

    private static final int MAXIMO_PARCELAS = 12;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext CALCULO = new MathContext(34, RoundingMode.HALF_EVEN);

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas, NivelClube nivelClube) {
        if (parcelas <= nivelClube.parcelasSemJuros()) {
            return semJuros(totalPedido, parcelas);
        }
        return comJuros(totalPedido, parcelas);
    }

    /** Sem juros: o valor final e o proprio total do pedido. */
    private ResultadoPagamento semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal parcela = Dinheiro.centavos(
                totalPedido.divide(BigDecimal.valueOf(parcelas), CALCULO));
        return new ResultadoPagamento(Dinheiro.centavos(totalPedido), parcela);
    }

    /** Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-parcelas). */
    private ResultadoPagamento comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, CALCULO);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, CALCULO));
        BigDecimal parcela = Dinheiro.centavos(
                totalPedido.multiply(TAXA_MENSAL).divide(divisor, CALCULO));
        BigDecimal totalFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcela);
    }
}
