package com.loja.pedidos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {
    @Id
    private String cpf;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private BigDecimal saldoCredito;

    @Column(nullable = false)
    private Instant criadoEm;
}
