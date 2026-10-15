package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Cupom {
    protected String codigo;

    protected Cupom(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public abstract boolean isAplicavel(BigDecimal subtotalProdutos, List<Integer> quantidades);

    public abstract BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Integer> quantidades,
                                                BigDecimal frete);

    private static final Map<String, Cupom> cupons = new HashMap<>();

    static {
        cupons.put("BEMVINDO10", new Bemvindo10());
        cupons.put("MENOS50", new Menos50());
        cupons.put("FRETEGRATIS", new FreteGratis());
        cupons.put("LEVE3PAGUE2", new Leve3Pague2());
    }

    public static Cupom fromCodigo(String codigo) {
        return cupons.get(codigo);
    }

    public static boolean existe(String codigo) {
        return cupons.containsKey(codigo);
    }

    private static class Bemvindo10 extends Cupom {
        Bemvindo10() {
            super("BEMVINDO10");
        }

        @Override
        public boolean isAplicavel(BigDecimal subtotalProdutos, List<Integer> quantidades) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Integer> quantidades, BigDecimal frete) {
            return subtotalProdutos.multiply(new BigDecimal("0.10"));
        }
    }

    private static class Menos50 extends Cupom {
        Menos50() {
            super("MENOS50");
        }

        @Override
        public boolean isAplicavel(BigDecimal subtotalProdutos, List<Integer> quantidades) {
            return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Integer> quantidades, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    }

    private static class FreteGratis extends Cupom {
        FreteGratis() {
            super("FRETEGRATIS");
        }

        @Override
        public boolean isAplicavel(BigDecimal subtotalProdutos, List<Integer> quantidades) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Integer> quantidades, BigDecimal frete) {
            return frete;
        }
    }

    private static class Leve3Pague2 extends Cupom {
        Leve3Pague2() {
            super("LEVE3PAGUE2");
        }

        @Override
        public boolean isAplicavel(BigDecimal subtotalProdutos, List<Integer> quantidades) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Integer> quantidades, BigDecimal precoUnitario) {
            BigDecimal desconto = BigDecimal.ZERO;
            return desconto;
        }

        public BigDecimal calcularDescontoComPrecos(List<Integer> quantidades, List<BigDecimal> precos) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (int i = 0; i < quantidades.size(); i++) {
                int quantidade = quantidades.get(i);
                BigDecimal preco = precos.get(i);
                int itensGratis = quantidade / 3;
                desconto = desconto.add(preco.multiply(new BigDecimal(itensGratis)));
            }
            return desconto;
        }
    }
}
