package loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static loja.checkout.dominio.Dinheiro.centavos;

public enum FormaPagamento {
    PIX {
        public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = centavos(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal ajuste = desconto.negate();
            BigDecimal valorParcela = centavos(totalPedido.add(ajuste));
            return new Pagamento(ajuste, valorParcela);
        }
        public boolean parcelasValidas(int parcelas) { return parcelas == 1; }
        public boolean atende(BigDecimal totalPedido) { return true; }
    },
    BOLETO {
        public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal ajuste = new BigDecimal("3.49");
            BigDecimal valorParcela = centavos(totalPedido.add(ajuste));
            return new Pagamento(ajuste, valorParcela);
        }
        public boolean parcelasValidas(int parcelas) { return parcelas == 1; }
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }
    },
    CARTAO {
        private static final BigDecimal TAXA = new BigDecimal("0.0199");

        public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
                return new Pagamento(centavos(BigDecimal.ZERO), valorParcela);
            }
            BigDecimal umMaisI = BigDecimal.ONE.add(TAXA);
            BigDecimal umMaisIaN = umMaisI.pow(parcelas);
            BigDecimal numerador = totalPedido.multiply(TAXA).multiply(umMaisIaN);
            BigDecimal denominador = umMaisIaN.subtract(BigDecimal.ONE);
            BigDecimal parcela = centavos(numerador.divide(denominador, 10, RoundingMode.HALF_EVEN));
            BigDecimal totalFinal = parcela.multiply(BigDecimal.valueOf(parcelas));
            BigDecimal ajuste = centavos(totalFinal.subtract(totalPedido));
            return new Pagamento(ajuste, parcela);
        }
        public boolean parcelasValidas(int parcelas) { return parcelas >= 1 && parcelas <= 12; }
        public boolean atende(BigDecimal totalPedido) { return true; }
    };

    public abstract Pagamento calcular(BigDecimal totalPedido, int parcelas);
    public abstract boolean parcelasValidas(int parcelas);
    public abstract boolean atende(BigDecimal totalPedido);
}
