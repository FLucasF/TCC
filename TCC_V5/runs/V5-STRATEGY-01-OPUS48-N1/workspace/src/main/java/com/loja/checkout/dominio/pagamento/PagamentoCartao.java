package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

/**
 * Cartão de crédito, de 1 a 12 parcelas. Até 3x é sem juros; de 4x a 12x há
 * juros de 1,99% ao mês pela tabela Price. Os dois jeitos de parcelar moram
 * aqui, que é o lugar do cartão.
 */
@Component
public class PagamentoCartao implements FormaPagamento {

    private static final int MAX_PARCELAS = 12;
    private static final int MAX_SEM_JUROS = 3;
    private static final BigDecimal TAXA = new BigDecimal("0.0199");
    private static final MathContext MC = new MathContext(34);

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_SEM_JUROS) {
            return semJuros(totalPedido, parcelas);
        }
        return comJuros(totalPedido, parcelas);
    }

    private ResultadoPagamento semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorParcela = Dinheiro.centavos(
                totalPedido.divide(BigDecimal.valueOf(parcelas), MC));
        return new ResultadoPagamento(totalPedido, valorParcela);
    }

    private ResultadoPagamento comJuros(BigDecimal totalPedido, int parcelas) {
        // parcela = total * taxa / (1 - (1 + taxa)^-n)
        BigDecimal fator = BigDecimal.ONE.add(TAXA).pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MC));
        BigDecimal valorParcela = Dinheiro.centavos(
                totalPedido.multiply(TAXA).divide(denominador, MC));
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        return new ResultadoPagamento(totalFinal, valorParcela);
    }
}
