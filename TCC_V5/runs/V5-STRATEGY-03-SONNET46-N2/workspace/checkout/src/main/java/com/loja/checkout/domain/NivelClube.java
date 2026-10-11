package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Optional;

public enum NivelClube {
    BRONZE {
        @Override public BigDecimal taxaCashback() { return BigDecimal.ZERO; }
        @Override public boolean freteGratis() { return false; }
        @Override public Optional<BigDecimal> limiarBrinde() { return Optional.empty(); }
    },
    PRATA {
        @Override public BigDecimal taxaCashback() { return new BigDecimal("0.02"); }
        @Override public boolean freteGratis() { return false; }
        @Override public Optional<BigDecimal> limiarBrinde() { return Optional.empty(); }
    },
    OURO {
        @Override public BigDecimal taxaCashback() { return new BigDecimal("0.05"); }
        @Override public boolean freteGratis() { return true; }
        @Override public Optional<BigDecimal> limiarBrinde() { return Optional.of(new BigDecimal("500.00")); }
    };

    public abstract BigDecimal taxaCashback();
    public abstract boolean freteGratis();
    public abstract Optional<BigDecimal> limiarBrinde();
}
