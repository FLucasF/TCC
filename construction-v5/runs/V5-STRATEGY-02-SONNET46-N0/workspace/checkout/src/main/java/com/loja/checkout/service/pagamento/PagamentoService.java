package com.loja.checkout.service.pagamento;

import com.loja.checkout.model.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Set;

@Service
public class PagamentoService {

    private static final Set<String> FORMAS_VALIDAS = Set.of("PIX", "CARTAO", "BOLETO");
    private static final BigDecimal TAXA_BOLETO = BigDecimal.valueOf(3.49);
    private static final BigDecimal LIMITE_BOLETO = BigDecimal.valueOf(1000.00);
    private static final BigDecimal TAXA_JUROS_MENSAL = BigDecimal.valueOf(0.0199);

    public ResultadoPagamento calcular(String forma, int parcelas, BigDecimal totalPedido) {
        if (forma == null || !FORMAS_VALIDAS.contains(forma)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        return switch (forma) {
            case "PIX" -> calcularPix(parcelas, totalPedido);
            case "BOLETO" -> calcularBoleto(parcelas, totalPedido);
            case "CARTAO" -> calcularCartao(parcelas, totalPedido);
            default -> throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        };
    }

    private ResultadoPagamento calcularPix(int parcelas, BigDecimal total) {
        if (parcelas != 1) throw new CheckoutException("PARCELAMENTO_INVALIDO");
        BigDecimal desconto = total.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = total.subtract(desconto);
        return new ResultadoPagamento(totalFinal, desconto.negate(), 1, totalFinal);
    }

    private ResultadoPagamento calcularBoleto(int parcelas, BigDecimal total) {
        if (parcelas != 1) throw new CheckoutException("PARCELAMENTO_INVALIDO");
        BigDecimal totalFinal = total.add(TAXA_BOLETO);
        if (totalFinal.compareTo(LIMITE_BOLETO) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        return new ResultadoPagamento(totalFinal, TAXA_BOLETO, 1, totalFinal);
    }

    private ResultadoPagamento calcularCartao(int parcelas, BigDecimal total) {
        if (parcelas < 1 || parcelas > 12) throw new CheckoutException("PARCELAMENTO_INVALIDO");
        if (parcelas <= 3) {
            BigDecimal parcela = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = parcela.multiply(BigDecimal.valueOf(parcelas));
            BigDecimal ajuste = totalFinal.subtract(total);
            return new ResultadoPagamento(totalFinal, ajuste, parcelas, parcela);
        }
        // Price table with monthly interest
        BigDecimal taxa = TAXA_JUROS_MENSAL;
        BigDecimal n = BigDecimal.valueOf(parcelas);
        // parcela = total × taxa / (1 - (1 + taxa)^-n)
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
        BigDecimal potencia = umMaisTaxa.pow(parcelas, new MathContext(20, RoundingMode.HALF_EVEN));
        BigDecimal fator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, 20, RoundingMode.HALF_EVEN));
        BigDecimal parcela = total.multiply(taxa).divide(fator, 2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = parcela.multiply(n);
        BigDecimal ajuste = totalFinal.subtract(total);
        return new ResultadoPagamento(totalFinal, ajuste, parcelas, parcela);
    }
}
