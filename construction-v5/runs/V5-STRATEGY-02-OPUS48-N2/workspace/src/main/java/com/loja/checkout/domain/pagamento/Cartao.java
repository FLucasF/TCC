package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.erro.CheckoutException;
import com.loja.checkout.domain.erro.ErroCheckout;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Cartao de credito, de 1 a 12 vezes. Ate 3x sem juros: o valor final e o proprio
 * total e a parcela e o total dividido pelo numero de parcelas. De 4x a 12x com
 * juros de 1,99% ao mes pela tabela Price: a parcela e arredondada e o valor final
 * e a parcela vezes o numero de parcelas.
 */
@Component
public class Cartao implements FormaPagamento {

    private static final int MAX_PARCELAS = 12;
    private static final int MAX_SEM_JUROS = 3;
    private static final BigDecimal TAXA = new BigDecimal("0.0199");
    private static final MathContext MC = new MathContext(20);

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas < 1 || parcelas > MAX_PARCELAS) {
            throw new CheckoutException(ErroCheckout.PARCELAMENTO_INVALIDO);
        }
    }

    @Override
    public void validarDisponibilidade(BigDecimal totalPedido) {
        // Sempre disponivel.
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_SEM_JUROS) {
            return semJuros(totalPedido, parcelas);
        }
        return comJuros(totalPedido, parcelas);
    }

    private ResultadoPagamento semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal parcela = Dinheiro.centavos(totalPedido.divide(BigDecimal.valueOf(parcelas), MC));
        return new ResultadoPagamento(Dinheiro.centavos(totalPedido), parcela);
    }

    private ResultadoPagamento comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA).pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MC));
        BigDecimal parcela = Dinheiro.centavos(totalPedido.multiply(TAXA).divide(denominador, MC));
        BigDecimal totalFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcela);
    }
}
