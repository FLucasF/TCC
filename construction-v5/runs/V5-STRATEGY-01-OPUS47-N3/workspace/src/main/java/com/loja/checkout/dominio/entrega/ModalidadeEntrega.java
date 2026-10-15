package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA {
        @Override public BigDecimal custoBruto(BigDecimal pesoKg) {
            return Dinheiro.aCentavos(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg)));
        }
        @Override public int prazoDias() { return 7; }
        @Override public boolean disponivelPara(BigDecimal pesoKg) { return true; }
    },
    EXPRESSA {
        @Override public BigDecimal custoBruto(BigDecimal pesoKg) {
            return Dinheiro.aCentavos(new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg)));
        }
        @Override public int prazoDias() { return 2; }
        @Override public boolean disponivelPara(BigDecimal pesoKg) { return true; }
    },
    RETIRADA_LOJA {
        @Override public BigDecimal custoBruto(BigDecimal pesoKg) { return Dinheiro.ZERO; }
        @Override public int prazoDias() { return 1; }
        @Override public boolean disponivelPara(BigDecimal pesoKg) { return true; }
    },
    MOTOBOY {
        @Override public BigDecimal custoBruto(BigDecimal pesoKg) {
            return Dinheiro.aCentavos(new BigDecimal("18.00"));
        }
        @Override public int prazoDias() { return 0; }
        @Override public boolean disponivelPara(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    public abstract BigDecimal custoBruto(BigDecimal pesoKg);
    public abstract int prazoDias();
    public abstract boolean disponivelPara(BigDecimal pesoKg);
}
