package com.loja.checkout.model;

import com.loja.checkout.util.Arredondador;

public enum Cupom {
    BEMVINDO10("BEMVINDO10") {
        @Override
        public Double calcularDesconto(Double subtotal, Double frete, Double pesoTotal) {
            return Arredondador.arredondar(subtotal * 0.10);
        }

        @Override
        public boolean eAplicavel(Double subtotal, Double frete, Double pesoTotal) {
            return true;
        }

        @Override
        public Double calcularFreteNoDesconto(Double frete) {
            return 0.0;
        }
    },
    MENOS50("MENOS50") {
        @Override
        public Double calcularDesconto(Double subtotal, Double frete, Double pesoTotal) {
            return 50.00;
        }

        @Override
        public boolean eAplicavel(Double subtotal, Double frete, Double pesoTotal) {
            return subtotal >= 300.00;
        }

        @Override
        public Double calcularFreteNoDesconto(Double frete) {
            return 0.0;
        }
    },
    FRETEGRATIS("FRETEGRATIS") {
        @Override
        public Double calcularDesconto(Double subtotal, Double frete, Double pesoTotal) {
            return Arredondador.arredondar(frete);
        }

        @Override
        public boolean eAplicavel(Double subtotal, Double frete, Double pesoTotal) {
            return true;
        }

        @Override
        public Double calcularFreteNoDesconto(Double frete) {
            return Arredondador.arredondar(frete);
        }
    },
    LEVE3PAGUE2("LEVE3PAGUE2") {
        @Override
        public Double calcularDesconto(Double subtotal, Double frete, Double pesoTotal) {
            return 0.0;
        }

        @Override
        public boolean eAplicavel(Double subtotal, Double frete, Double pesoTotal) {
            return true;
        }

        @Override
        public Double calcularFreteNoDesconto(Double frete) {
            return 0.0;
        }
    };

    private final String codigo;

    Cupom(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public abstract Double calcularDesconto(Double subtotal, Double frete, Double pesoTotal);

    public abstract boolean eAplicavel(Double subtotal, Double frete, Double pesoTotal);

    public abstract Double calcularFreteNoDesconto(Double frete);

    public static Cupom fromString(String cupom) {
        if (cupom == null) {
            return null;
        }
        for (Cupom c : Cupom.values()) {
            if (c.codigo.equals(cupom)) {
                return c;
            }
        }
        return null;
    }
}
