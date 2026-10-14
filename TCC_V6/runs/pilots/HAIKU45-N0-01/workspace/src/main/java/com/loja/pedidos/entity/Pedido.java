package com.loja.pedidos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cpfCliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoPedido situacao;

    @Column(nullable = false)
    private Instant criadoEm;

    private Instant pagoEm;
    private String idTransacao;
    private String codigoRastreio;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String resumoJson;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pedido_id")
    @Builder.Default
    private List<ItemPedido> itens = new ArrayList<>();

    public enum SituacaoPedido {
        AGUARDANDO_PAGAMENTO,
        PAGO,
        ENVIADO,
        ENTREGUE,
        CANCELADO
    }
}
