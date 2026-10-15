package com.loja.checkout.domain;

import java.math.BigDecimal;

public enum FormaPagamento {
    PIX("PIX", 1, 1),
    CARTAO("CARTAO", 1, 12),
    BOLETO("BOLETO", 1, 1);

    private final String codigo;
    private final int minParcelas;
    private final int maxParcelas;

    FormaPagamento(String codigo, int minParcelas, int maxParcelas) {
        this.codigo = codigo;
        this.minParcelas = minParcelas;
        this.maxParcelas = maxParcelas;
    }

    public String getCodigo() {
        return codigo;
    }

    public int getMinParcelas() {
        return minParcelas;
    }

    public int getMaxParcelas() {
        return maxParcelas;
    }

    public static FormaPagamento fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (FormaPagamento f : values()) {
            if (f.codigo.equals(codigo)) {
                return f;
            }
        }
        return null;
    }

    public boolean isDisponivel(BigDecimal totalPedido) {
        if (this == BOLETO) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }
        return true;
    }

    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        return switch (this) {
            case PIX -> totalPedido.multiply(new BigDecimal("0.05")).negate();
            case BOLETO -> new BigDecimal("3.49");
            case CARTAO -> calcularJurosCarta(totalPedido, parcelas);
        };
    }

    private BigDecimal calcularJurosCarta(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return BigDecimal.ZERO;
        }
        BigDecimal taxaMensal = new BigDecimal("0.0199");
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxaMensal);

        BigDecimal potencia = BigDecimal.ONE;
        for (int i = 0; i < parcelas; i++) {
            potencia = potencia.multiply(umMaisTaxa);
        }

        BigDecimal numerador = totalPedido.multiply(taxaMensal);
        BigDecimal denominador = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(potencia, 15, java.math.RoundingMode.HALF_EVEN)
        );
        BigDecimal parcela = numerador.divide(denominador, 10, java.math.RoundingMode.HALF_EVEN);
        BigDecimal totalComJuros = parcela.multiply(new BigDecimal(parcelas));
        return totalComJuros.subtract(totalPedido);
    }
}
