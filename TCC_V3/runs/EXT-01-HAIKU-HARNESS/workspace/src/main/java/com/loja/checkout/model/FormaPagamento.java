package com.loja.checkout.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum FormaPagamento {
    PIX {
        @Override
        public boolean permite(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Arredonda.round(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            BigDecimal valorParcela = Arredonda.round(totalFinal.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));
            BigDecimal totalComParcelas = valorParcela.multiply(new BigDecimal(parcelas));

            return new ResultadoPagamento(
                totalComParcelas,
                desconto.negate(),
                valorParcela
            );
        }
    },
    CARTAO {
        @Override
        public boolean permite(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = Arredonda.round(totalPedido.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));
                BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
                return new ResultadoPagamento(
                    totalFinal,
                    BigDecimal.ZERO,
                    valorParcela
                );
            } else {
                BigDecimal taxa = new BigDecimal("0.0199");
                BigDecimal fator = BigDecimal.ONE.add(taxa);
                BigDecimal potencia = fator.pow(parcelas);
                BigDecimal numerador = taxa.multiply(potencia);
                BigDecimal denominador = potencia.subtract(BigDecimal.ONE);

                BigDecimal valorParcela = Arredonda.round(
                    totalPedido.multiply(numerador).divide(denominador, 10, RoundingMode.HALF_EVEN)
                );
                BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
                BigDecimal juros = totalFinal.subtract(totalPedido);

                return new ResultadoPagamento(
                    totalFinal,
                    juros,
                    valorParcela
                );
            }
        }
    },
    BOLETO {
        @Override
        public boolean permite(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal tarifa = new BigDecimal("3.49");
            BigDecimal totalFinal = totalPedido.add(tarifa);
            BigDecimal valorParcela = Arredonda.round(totalFinal.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));
            BigDecimal totalComParcelas = valorParcela.multiply(new BigDecimal(parcelas));

            return new ResultadoPagamento(
                totalComParcelas,
                tarifa,
                valorParcela
            );
        }
    };

    public abstract boolean permite(int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    public record ResultadoPagamento(
        BigDecimal totalFinal,
        BigDecimal ajuste,
        BigDecimal valorParcela
    ) {}
}
