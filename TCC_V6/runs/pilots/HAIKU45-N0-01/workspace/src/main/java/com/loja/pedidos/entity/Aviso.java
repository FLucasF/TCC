package com.loja.pedidos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "avisos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aviso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cpfCliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAviso tipo;

    @Column(nullable = false)
    private Long pedidoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoAviso situacao;

    @Column(nullable = false)
    private Instant criadoEm;

    public enum TipoAviso {
        PAGAMENTO_APROVADO,
        PEDIDO_ENVIADO,
        PEDIDO_CANCELADO
    }

    public enum SituacaoAviso {
        NOVO,
        LIDO
    }
}
