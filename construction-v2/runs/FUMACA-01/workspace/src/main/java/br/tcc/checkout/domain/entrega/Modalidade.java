package br.tcc.checkout.domain.entrega;

public enum Modalidade {
    ECONOMICA(12.00, 2.00, 7) {
        @Override
        public double calcularFrete(double pesoKg) {
            return 12.00 + (2.00 * pesoKg);
        }
    },
    EXPRESSA(25.00, 4.50, 2) {
        @Override
        public double calcularFrete(double pesoKg) {
            return 25.00 + (4.50 * pesoKg);
        }
    },
    RETIRADA_LOJA(0.00, 0.00, 1) {
        @Override
        public double calcularFrete(double pesoKg) {
            return 0.00;
        }
    },
    MOTOBOY(18.00, 0.00, 0) {
        @Override
        public double calcularFrete(double pesoKg) {
            return 18.00;
        }

        @Override
        public boolean ehDisponivelPara(double pesoKg) {
            return pesoKg <= 5.0;
        }
    };

    private final double precoBase;
    private final double precoKg;
    private final int prazo;

    Modalidade(double precoBase, double precoKg, int prazo) {
        this.precoBase = precoBase;
        this.precoKg = precoKg;
        this.prazo = prazo;
    }

    public abstract double calcularFrete(double pesoKg);

    public int getPrazo() {
        return prazo;
    }

    public boolean ehDisponivelPara(double pesoKg) {
        return true;
    }
}
