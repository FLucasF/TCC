package com.loja.checkout.pagamento;

import com.loja.checkout.comum.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cartão de crédito de 1 a 12 vezes. Até 3x sem juros (valor final = total);
 * de 4x a 12x com juros de 1,99% ao mês pela tabela Price.
 */
@Component
public class CartaoPagamento implements FormaPagamento {

    private static final int MAX_PARCELAS = 12;
    private static final int MAX_SEM_JUROS = 3;
    private static final BigDecimal TAXA = new BigDecimal("0.0199");
    private static final int ESCALA_CALCULO = 12;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcula(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_SEM_JUROS) {
            BigDecimal parcela = Dinheiro.centavos(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), ESCALA_CALCULO, RoundingMode.HALF_EVEN));
            return new ResultadoPagamento(Dinheiro.centavos(totalPedido), parcela);
        }
        BigDecimal parcela = parcelaComJuros(totalPedido, parcelas);
        BigDecimal valorFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(valorFinal, parcela);
    }

    // Tabela Price: parcela = total * i / (1 - (1 + i)^-n).
    // Reescrito com potência inteira: parcela = total * i * p / (p - 1), p = (1 + i)^n.
    private BigDecimal parcelaComJuros(BigDecimal total, int parcelas) {
        BigDecimal p = BigDecimal.ONE.add(TAXA).pow(parcelas);
        BigDecimal numerador = total.multiply(TAXA).multiply(p);
        BigDecimal denominador = p.subtract(BigDecimal.ONE);
        BigDecimal parcela = numerador.divide(denominador, ESCALA_CALCULO, RoundingMode.HALF_EVEN);
        return Dinheiro.centavos(parcela);
    }
}
