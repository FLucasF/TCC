package br.tcc.checkout.domain.cupom;

import br.tcc.checkout.domain.Arredondador;

public enum Cupom {
    BEMVINDO10("BEMVINDO10") {
        @Override
        public double aplicar(double subtotal, double frete) {
            return Arredondador.arredondar(subtotal * 0.10);
        }

        @Override
        public boolean eAplicavel(double subtotal, double frete) {
            return true;
        }
    },
    MENOS50("MENOS50") {
        @Override
        public double aplicar(double subtotal, double frete) {
            return 50.00;
        }

        @Override
        public boolean eAplicavel(double subtotal, double frete) {
            return subtotal >= 300.00;
        }
    },
    FRETEGRATIS("FRETEGRATIS") {
        @Override
        public double aplicar(double subtotal, double frete) {
            return Arredondador.arredondar(frete);
        }

        @Override
        public boolean eAplicavel(double subtotal, double frete) {
            return true;
        }
    },
    LEVE3PAGUE2("LEVE3PAGUE2") {
        @Override
        public double aplicar(double subtotal, double frete) {
            return 0.0;
        }

        @Override
        public boolean eAplicavel(double subtotal, double frete) {
            return true;
        }
    }
;

    private final String codigo;

    Cupom(String codigo) {
        this.codigo = codigo;
    }

    public abstract double aplicar(double subtotal, double frete);

    public abstract boolean eAplicavel(double subtotal, double frete);

    public String getCodigo() {
        return codigo;
    }

    public static Cupom porCodigo(String codigo) {
        if (codigo == null) return null;
        for (Cupom cupom : values()) {
            if (cupom.codigo.equals(codigo)) {
                return cupom;
            }
        }
        return null;
    }
}
