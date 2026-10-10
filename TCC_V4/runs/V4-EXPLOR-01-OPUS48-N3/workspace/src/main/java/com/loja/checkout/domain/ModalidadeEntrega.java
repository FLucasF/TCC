package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Forma de entrega. Cada opção tem seu jeito de cobrar, seu prazo e suas
 * limitações; cada uma mora no seu próprio membro. Entra opção nova = novo
 * membro. O frete devolvido aqui é o frete "cheio" da modalidade; a isenção do
 * clube (OURO) é aplicada depois, por NivelClube.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        BigDecimal freteCheio(BigDecimal pesoKg) {
            return base(12).add(porKg("2", pesoKg));
        }
    },
    EXPRESSA(2) {
        @Override
        BigDecimal freteCheio(BigDecimal pesoKg) {
            return base(25).add(porKg("4.50", pesoKg));
        }
    },
    RETIRADA_LOJA(1) {
        @Override
        BigDecimal freteCheio(BigDecimal pesoKg) {
            return base(0);
        }
    },
    MOTOBOY(0) {
        @Override
        BigDecimal freteCheio(BigDecimal pesoKg) {
            return base(18);
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    abstract BigDecimal freteCheio(BigDecimal pesoKg);

    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.centavos(freteCheio(pesoKg));
    }

    public int prazoDias() {
        return prazoDias;
    }

    public boolean disponivel(BigDecimal pesoKg) {
        return true;
    }

    static BigDecimal base(int reais) {
        return new BigDecimal(reais);
    }

    static BigDecimal porKg(String tarifa, BigDecimal pesoKg) {
        return new BigDecimal(tarifa).multiply(pesoKg);
    }

    public static Optional<ModalidadeEntrega> resolver(String codigo) {
        return Catalogo.achar(ModalidadeEntrega.class, codigo);
    }
}
