package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Cartao de credito: de 1x a 3x sem juros e de 4x a 12x com juros de 1,99% ao mes
 * pela tabela Price.
 */
@Component
public class PagamentoCartao implements FormaPagamento {

    private static final int PARCELA_MINIMA = 1;
    private static final int PARCELA_MAXIMA = 12;
    private static final int MAXIMO_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= PARCELA_MINIMA && parcelas <= PARCELA_MAXIMA;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAXIMO_SEM_JUROS) {
            return semJuros(totalPedido, parcelas);
        }
        return comJuros(totalPedido, parcelas);
    }

    /** Sem juros o valor final e o proprio total do pedido. */
    private ResultadoPagamento semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorParcela = totalPedido.divide(
                BigDecimal.valueOf(parcelas), Dinheiro.CASAS, Dinheiro.MODO);
        return new ResultadoPagamento(Dinheiro.centavos(totalPedido), parcelas, valorParcela);
    }

    /**
     * Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-parcelas).
     * O valor final e a parcela arredondada multiplicada pelo numero de parcelas.
     */
    private ResultadoPagamento comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal valorParcela = Dinheiro.centavos(totalPedido
                .multiply(TAXA_MENSAL)
                .multiply(fator)
                .divide(fator.subtract(BigDecimal.ONE), Dinheiro.PRECISAO_CALCULO));
        BigDecimal totalFinal = Dinheiro.centavos(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcelas, valorParcela);
    }
}
