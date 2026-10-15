package com.loja.checkout.pagamento;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.model.Resumo;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("CARTAO")
public class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int MINIMO_PARCELAS = 1;
    private static final int MAXIMO_PARCELAS = 12;

    @Override
    public void validarDisponibilidade(Resumo resumo) {
    }

    @Override
    public void validarParcelas(Integer parcelas) {
        if (parcelas == null || parcelas < MINIMO_PARCELAS || parcelas > MAXIMO_PARCELAS) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, Integer parcelas) {
        BigDecimal valorFinal = calcularValorFinal(totalPedido, parcelas);
        return valorFinal.subtract(totalPedido);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, Integer parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return totalPedido.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS_MENSAL);
        BigDecimal umMaisTaxaPotencia = umMaisTaxa.pow(parcelas);
        BigDecimal fator = TAXA_JUROS_MENSAL.multiply(umMaisTaxaPotencia);
        BigDecimal divisor = umMaisTaxaPotencia.subtract(BigDecimal.ONE);

        return totalPedido
            .multiply(fator)
            .divide(divisor, 2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularValorFinal(BigDecimal totalPedido, Integer parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return totalPedido;
        }

        BigDecimal valorParcela = calcularValorParcela(totalPedido, parcelas);
        return valorParcela.multiply(new BigDecimal(parcelas));
    }
}
