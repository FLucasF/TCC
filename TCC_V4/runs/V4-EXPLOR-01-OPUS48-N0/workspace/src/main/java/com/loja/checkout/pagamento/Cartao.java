package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

/**
 * Cartão de crédito: de 1 a 12 vezes.
 *
 * <p>Até 3x é sem juros (o valor final é o próprio total do pedido). De 4x a 12x
 * tem juros de 1,99% ao mês, pela tabela Price:
 * {@code parcela = total × taxa ÷ (1 − (1 + taxa)^−n)}.
 */
@Component
public class Cartao implements FormaPagamento {

    private static final int PARCELA_MINIMA = 1;
    private static final int PARCELA_MAXIMA = 12;
    private static final int MAXIMO_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= PARCELA_MINIMA && parcelas <= PARCELA_MAXIMA;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAXIMO_SEM_JUROS) {
            return semJuros(totalPedido, parcelas);
        }
        return comJuros(totalPedido, parcelas);
    }

    private ResultadoPagamento semJuros(BigDecimal totalPedido, int parcelas) {
        // O valor final é o próprio total; só a parcela é dividida e arredondada.
        BigDecimal valorParcela = Dinheiro.centavos(
                totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL64));
        return new ResultadoPagamento(parcelas, valorParcela, totalPedido, Dinheiro.ZERO);
    }

    private ResultadoPagamento comJuros(BigDecimal totalPedido, int parcelas) {
        // parcela = total × taxa × p / (p − 1), onde p = (1 + taxa)^n
        BigDecimal p = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal numerador = totalPedido.multiply(TAXA_MENSAL).multiply(p);
        BigDecimal denominador = p.subtract(BigDecimal.ONE);
        BigDecimal valorParcela = Dinheiro.centavos(
                numerador.divide(denominador, MathContext.DECIMAL64));

        BigDecimal totalFinal = Dinheiro.centavos(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        BigDecimal ajuste = Dinheiro.centavos(totalFinal.subtract(totalPedido));
        return new ResultadoPagamento(parcelas, valorParcela, totalFinal, ajuste);
    }
}
