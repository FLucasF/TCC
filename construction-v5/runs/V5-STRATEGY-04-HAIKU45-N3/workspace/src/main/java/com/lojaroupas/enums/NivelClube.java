package com.lojaroupas.enums;

public enum NivelClube {
    BRONZE {
        @Override
        public Double calcularCredito(Double subtotal) {
            return 0.0;
        }

        @Override
        public boolean temFreteGratis() {
            return false;
        }

        @Override
        public boolean temBrinde(Double subtotal) {
            return false;
        }
    },
    PRATA {
        @Override
        public Double calcularCredito(Double subtotal) {
            return subtotal * 0.02;
        }

        @Override
        public boolean temFreteGratis() {
            return false;
        }

        @Override
        public boolean temBrinde(Double subtotal) {
            return false;
        }
    },
    OURO {
        @Override
        public Double calcularCredito(Double subtotal) {
            return subtotal * 0.05;
        }

        @Override
        public boolean temFreteGratis() {
            return true;
        }

        @Override
        public boolean temBrinde(Double subtotal) {
            return subtotal > 500.00;
        }
    };

    public abstract Double calcularCredito(Double subtotal);
    public abstract boolean temFreteGratis();
    public abstract boolean temBrinde(Double subtotal);

    public static NivelClube fromString(String value) {
        try {
            return NivelClube.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
