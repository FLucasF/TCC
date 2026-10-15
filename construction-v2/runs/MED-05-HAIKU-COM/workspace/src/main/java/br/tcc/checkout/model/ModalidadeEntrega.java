package br.tcc.checkout.model;

public enum ModalidadeEntrega {
    ECONOMICA(12.00, 2.00, 7) {
        @Override
        public Double calcularFrete(Double pesoKg) {
            return 12.00 + (2.00 * pesoKg);
        }
    },
    EXPRESSA(25.00, 4.50, 2) {
        @Override
        public Double calcularFrete(Double pesoKg) {
            return 25.00 + (4.50 * pesoKg);
        }
    },
    RETIRADA_LOJA(0.00, 0.00, 1) {
        @Override
        public Double calcularFrete(Double pesoKg) {
            return 0.00;
        }
    },
    MOTOBOY(18.00, 0.00, 0) {
        @Override
        public Double calcularFrete(Double pesoKg) {
            return 18.00;
        }

        @Override
        public boolean temLimitacao() {
            return true;
        }

        @Override
        public Double getLimitePeso() {
            return 5.0;
        }
    };

    private final Double taxaBase;
    private final Double taxaPorKg;
    private final Integer prazoEmDias;

    ModalidadeEntrega(Double taxaBase, Double taxaPorKg, Integer prazoEmDias) {
        this.taxaBase = taxaBase;
        this.taxaPorKg = taxaPorKg;
        this.prazoEmDias = prazoEmDias;
    }

    public abstract Double calcularFrete(Double pesoKg);

    public Integer getPrazoEmDias() {
        return prazoEmDias;
    }

    public boolean temLimitacao() {
        return false;
    }

    public Double getLimitePeso() {
        return null;
    }
}
