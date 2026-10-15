package com.loja.checkout.pagamento;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;

public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelaValida(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public AjustePagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            return new AjustePagamento(totalFinal, totalFinal, desconto.negate());
        }
    },

    BOLETO {
        @Override
        public boolean parcelaValida(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public AjustePagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal tarifa = new BigDecimal("3.49");
            BigDecimal totalFinal = totalPedido.add(tarifa);
            return new AjustePagamento(totalFinal, totalFinal, tarifa);
        }
    },

    CARTAO {
        @Override
        public boolean parcelaValida(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public AjustePagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = Dinheiro.arredondar(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
                return new AjustePagamento(totalPedido, valorParcela, new BigDecimal("0.00"));
            }

            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal fator = BigDecimal.ONE.add(taxa).pow(parcelas, MathContext.DECIMAL128);
            BigDecimal valorParcela = Dinheiro.arredondar(
                    totalPedido.multiply(taxa).multiply(fator)
                            .divide(fator.subtract(BigDecimal.ONE), MathContext.DECIMAL128));
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new AjustePagamento(totalFinal, valorParcela, ajuste);
        }
    };

    public abstract boolean parcelaValida(int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedido);

    public abstract AjustePagamento calcular(BigDecimal totalPedido, int parcelas);

    public static Optional<FormaPagamento> fromCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(codigo));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
