package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class CalculadoraPagamento {

    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
    private static final BigDecimal JUROS_CARTAO = new BigDecimal("0.0199");
    private static final int MAX_PARCELAS_SEM_JUROS = 3;
    private static final int MAX_PARCELAS_CARTAO = 12;
    private static final BigDecimal UM = BigDecimal.ONE;
    private static final MathContext PRECISAO = new MathContext(50, RoundingMode.HALF_EVEN);

    private CalculadoraPagamento() {
    }

    public static Pagamento calcular(FormaPagamento forma, BigDecimal totalPedido, int parcelas) {
        return switch (forma) {
            case PIX -> pix(totalPedido, parcelas);
            case BOLETO -> boleto(totalPedido, parcelas);
            case CARTAO -> cartao(totalPedido, parcelas);
        };
    }

    private static Pagamento pix(BigDecimal total, int parcelas) {
        exigirParcelaUnica(parcelas);
        BigDecimal ajuste = Dinheiro.centavos(total.multiply(DESCONTO_PIX)).negate();
        BigDecimal totalFinal = total.add(ajuste);
        return new Pagamento(ajuste, totalFinal, 1, totalFinal);
    }

    private static Pagamento boleto(BigDecimal total, int parcelas) {
        exigirParcelaUnica(parcelas);
        if (total.compareTo(LIMITE_BOLETO) > 0) {
            throw new ErroPedidoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        BigDecimal totalFinal = total.add(TARIFA_BOLETO);
        return new Pagamento(TARIFA_BOLETO, totalFinal, 1, totalFinal);
    }

    private static Pagamento cartao(BigDecimal total, int parcelas) {
        if (parcelas < 1 || parcelas > MAX_PARCELAS_CARTAO) {
            throw new ErroPedidoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new Pagamento(Dinheiro.ZERO, total, parcelas, valorParcela);
        }
        BigDecimal fatorAcumulado = UM.add(JUROS_CARTAO).pow(parcelas, PRECISAO);
        BigDecimal denominador = UM.subtract(UM.divide(fatorAcumulado, PRECISAO));
        BigDecimal valorParcela = Dinheiro.centavos(total.multiply(JUROS_CARTAO).divide(denominador, PRECISAO));
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        return new Pagamento(totalFinal.subtract(total), totalFinal, parcelas, valorParcela);
    }

    private static void exigirParcelaUnica(int parcelas) {
        if (parcelas != 1) {
            throw new ErroPedidoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }
}
