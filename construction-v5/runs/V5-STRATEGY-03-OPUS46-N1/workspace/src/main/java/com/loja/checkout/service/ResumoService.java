package com.loja.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.regiao.Regiao;

@Service
public class ResumoService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveisClube;
    private final Map<String, FormaPagamento> formasPagamento;

    public ResumoService(List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
                         List<NivelClube> niveisClube, List<FormaPagamento> formasPagamento) {
        this.modalidades = modalidades.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::getCodigo, Function.identity()));
        this.cupons = cupons.stream()
                .collect(Collectors.toMap(Cupom::getCodigo, Function.identity()));
        this.niveisClube = niveisClube.stream()
                .collect(Collectors.toMap(NivelClube::getCodigo, Function.identity()));
        this.formasPagamento = formasPagamento.stream()
                .collect(Collectors.toMap(FormaPagamento::getCodigo, Function.identity()));
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        validarItens(pedido.itens());
        BigDecimal subtotal = calcularSubtotal(pedido.itens());
        BigDecimal pesoTotal = calcularPesoTotal(pedido.itens());

        NivelClube nivel = niveisClube.get(pedido.nivelClube());
        if (nivel == null) throw new CheckoutException("NIVEL_CLUBE_INVALIDO");

        Regiao regiao = Regiao.fromCodigo(pedido.regiao());
        if (regiao == null) throw new CheckoutException("REGIAO_INVALIDA");

        ModalidadeEntrega modalidade = modalidades.get(pedido.modalidadeEntrega());
        if (modalidade == null) throw new CheckoutException("MODALIDADE_INVALIDA");
        if (!modalidade.isDisponivel(pesoTotal)) throw new CheckoutException("MODALIDADE_INDISPONIVEL");

        Cupom cupom = null;
        if (pedido.cupom() != null && !pedido.cupom().isEmpty()) {
            cupom = cupons.get(pedido.cupom());
            if (cupom == null) throw new CheckoutException("CUPOM_INVALIDO");
            if (!cupom.isAplicavel(subtotal)) throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        FormaPagamento formaPagamento = formasPagamento.get(pedido.formaPagamento());
        if (formaPagamento == null) throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");

        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        if (!formaPagamento.isParcelasValidas(parcelas)) throw new CheckoutException("PARCELAMENTO_INVALIDO");

        BigDecimal frete = nivel.isFreteGratis()
                ? BigDecimal.ZERO.setScale(2)
                : modalidade.calcularFrete(pesoTotal);

        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(subtotal, frete, pedido.itens())
                : BigDecimal.ZERO.setScale(2);

        BigDecimal seguro = regiao.calcularSeguro(subtotal);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro)
                .setScale(2, RoundingMode.HALF_EVEN);

        if (!formaPagamento.isDisponivel(totalPedido)) throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido).setScale(2, RoundingMode.HALF_EVEN);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.getPrazoDias(),
                seguro,
                ajuste,
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                nivel.calcularCredito(subtotal),
                nivel.temBrinde(subtotal)
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) throw new CheckoutException("PEDIDO_INVALIDO");
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(new BigDecimal(item.quantidade())));
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
}
