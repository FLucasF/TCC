package com.loja.checkout.service.pagamento;

import com.loja.checkout.util.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PagamentoService {

    private static final Set<String> FORMAS_VALIDAS = Set.of("PIX", "CARTAO", "BOLETO");

    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
    private static final BigDecimal TAXA_JUROS_CARTAO = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;

    public boolean isFormaValida(String formaPagamento) {
        return FORMAS_VALIDAS.contains(formaPagamento);
    }

    public boolean isParcelamentoValido(String formaPagamento, int parcelas) {
        return switch (formaPagamento) {
            case "PIX", "BOLETO" -> parcelas == 1;
            case "CARTAO" -> parcelas >= 1 && parcelas <= 12;
            default -> false;
        };
    }

    public boolean isDisponivel(String formaPagamento, BigDecimal totalPedido) {
        if ("BOLETO".equals(formaPagamento)) {
            return totalPedido.compareTo(LIMITE_BOLETO) <= 0;
        }
        return true;
    }

    public ResultadoPagamento calcular(String formaPagamento, BigDecimal totalPedido, int parcelas) {
        return switch (formaPagamento) {
            case "PIX" -> calcularPix(totalPedido);
            case "BOLETO" -> calcularBoleto(totalPedido);
            case "CARTAO" -> calcularCartao(totalPedido, parcelas);
            default -> throw new IllegalStateException("Forma de pagamento desconhecida: " + formaPagamento);
        };
    }

    private ResultadoPagamento calcularPix(BigDecimal totalPedido) {
        BigDecimal desconto = Money.round(totalPedido.multiply(DESCONTO_PIX));
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        return new ResultadoPagamento(desconto.negate(), totalFinal, 1, totalFinal);
    }

    private ResultadoPagamento calcularBoleto(BigDecimal totalPedido) {
        BigDecimal totalFinal = totalPedido.add(TARIFA_BOLETO);
        return new ResultadoPagamento(TARIFA_BOLETO, totalFinal, 1, totalFinal);
    }

    private ResultadoPagamento calcularCartao(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Money.round(totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
            return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalPedido, parcelas, valorParcela);
        }

        BigDecimal fator = BigDecimal.ONE.add(TAXA_JUROS_CARTAO);
        BigDecimal fatorElevado = fator.pow(parcelas);
        BigDecimal numerador = totalPedido.multiply(TAXA_JUROS_CARTAO).multiply(fatorElevado);
        BigDecimal denominador = fatorElevado.subtract(BigDecimal.ONE);
        BigDecimal valorParcela = Money.round(numerador.divide(denominador, 10, RoundingMode.HALF_EVEN));
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, parcelas, valorParcela);
    }
}
