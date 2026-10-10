package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

/**
 * Cartão de crédito, de 1 a 12 vezes. Até 3x é sem juros; de 4x a 12x há juros
 * de 1,99% ao mês pela tabela Price. São os dois únicos casos descritos, e cada
 * um fecha o valor de um jeito próprio.
 */
@Component
public class Cartao implements FormaPagamento {

    private static final int MAX_SEM_JUROS = 3;
    private static final int MAX_PARCELAS = 12;
    private static final BigDecimal TAXA = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = MathContext.DECIMAL64;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public PagamentoResultado calcular(BigDecimal totalPedido, int parcelas) {
        return parcelas <= MAX_SEM_JUROS
                ? semJuros(totalPedido, parcelas)
                : comJuros(totalPedido, parcelas);
    }

    /** Valor final é o próprio total; a parcela é o total dividido pelas vezes. */
    private PagamentoResultado semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.centavos(totalPedido);
        BigDecimal valorParcela = Dinheiro.centavos(
                totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
        return new PagamentoResultado(totalFinal, valorParcela);
    }

    /** Parcela pela tabela Price; valor final é a parcela vezes o número de vezes. */
    private PagamentoResultado comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal base = BigDecimal.ONE.add(TAXA);
        BigDecimal fator = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(base.pow(parcelas), PRECISAO));
        BigDecimal parcela = totalPedido.multiply(TAXA).divide(fator, PRECISAO);
        BigDecimal valorParcela = Dinheiro.centavos(parcela);
        BigDecimal totalFinal = Dinheiro.centavos(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        return new PagamentoResultado(totalFinal, valorParcela);
    }
}
