package com.loja.checkout.service;

import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ResumoService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveisClube;

    public ResumoService(List<ModalidadeEntrega> modalidadesList,
                         List<Cupom> cuponsList,
                         List<NivelClube> niveisClubeList) {
        this.modalidades = modalidadesList.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
        this.cupons = cuponsList.stream()
                .collect(Collectors.toMap(Cupom::codigo, Function.identity()));
        this.niveisClube = niveisClubeList.stream()
                .collect(Collectors.toMap(NivelClube::codigo, Function.identity()));
    }

    public Object calcular(PedidoRequest pedido) {
        if (!itensValidos(pedido.itens())) {
            return Map.of("erro", "PEDIDO_INVALIDO");
        }

        NivelClube nivel = niveisClube.get(pedido.nivelClube());
        if (nivel == null) {
            return Map.of("erro", "NIVEL_CLUBE_INVALIDO");
        }

        Regiao regiao = parseRegiao(pedido.regiao());
        if (regiao == null) {
            return Map.of("erro", "REGIAO_INVALIDA");
        }

        ModalidadeEntrega modalidade = modalidades.get(pedido.modalidadeEntrega());
        if (modalidade == null) {
            return Map.of("erro", "MODALIDADE_INVALIDA");
        }

        BigDecimal pesoTotal = calcularPesoTotal(pedido.itens());
        if (!modalidade.disponivel(pesoTotal)) {
            return Map.of("erro", "MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = calcularSubtotal(pedido.itens());

        Cupom cupom = null;
        if (pedido.cupom() != null && !pedido.cupom().isBlank()) {
            cupom = cupons.get(pedido.cupom());
            if (cupom == null) {
                return Map.of("erro", "CUPOM_INVALIDO");
            }
            if (!cupom.aplicavel(pedido.itens(), subtotal)) {
                return Map.of("erro", "CUPOM_NAO_APLICAVEL");
            }
        }

        FormaPagamento formaPagamento = parseFormaPagamento(pedido.formaPagamento());
        if (formaPagamento == null) {
            return Map.of("erro", "FORMA_PAGAMENTO_INVALIDA");
        }

        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        if (!formaPagamento.parcelasValidas(parcelas)) {
            return Map.of("erro", "PARCELAMENTO_INVALIDO");
        }

        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        if (nivel.freteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            descontoCupom = cupom.calcularDesconto(pedido.itens(), subtotal, frete);
        }

        BigDecimal seguro = regiao.calcularSeguro(subtotal);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!formaPagamento.disponivel(totalPedido)) {
            return Map.of("erro", "FORMA_PAGAMENTO_INDISPONIVEL");
        }

        FormaPagamento.ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela(),
                nivel.calcularCredito(subtotal),
                nivel.temBrinde(subtotal)
        );
    }

    private boolean itensValidos(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            return false;
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }
            if (item.quantidade() == null || item.quantidade() <= 0) {
                return false;
            }
            if (item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }
        }
        return true;
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(
                    item.precoUnitario().multiply(new BigDecimal(item.quantidade()))
            );
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }

    private Regiao parseRegiao(String valor) {
        if (valor == null) return null;
        try {
            return Regiao.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private FormaPagamento parseFormaPagamento(String valor) {
        if (valor == null) return null;
        try {
            return FormaPagamento.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
