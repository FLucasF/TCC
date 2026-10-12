package com.loja.checkout.service;

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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, FormaPagamento> formasPagamento;
    private final Map<String, NivelClube> niveisClube;

    public CheckoutService(List<ModalidadeEntrega> modalidadesList,
                           List<Cupom> cuponsList,
                           List<FormaPagamento> formasPagamentoList,
                           List<NivelClube> niveisClubeLista) {
        this.modalidades = modalidadesList.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
        this.cupons = cuponsList.stream()
                .collect(Collectors.toMap(Cupom::codigo, Function.identity()));
        this.formasPagamento = formasPagamentoList.stream()
                .collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
        this.niveisClube = niveisClubeLista.stream()
                .collect(Collectors.toMap(NivelClube::codigo, Function.identity()));
    }

    public ResumoResponse calcularResumo(PedidoRequest pedido) {
        validarItens(pedido.itens());

        BigDecimal subtotal = calcularSubtotal(pedido.itens());
        BigDecimal pesoTotal = calcularPeso(pedido.itens());

        NivelClube nivel = buscar(pedido.nivelClube(), niveisClube, "NIVEL_CLUBE_INVALIDO");

        Regiao regiao = buscarRegiao(pedido.regiao());

        ModalidadeEntrega modalidade = buscar(pedido.modalidadeEntrega(), modalidades, "MODALIDADE_INVALIDA");
        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        if (nivel.freteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (pedido.cupom() != null && !pedido.cupom().isBlank()) {
            Cupom cupom = buscar(pedido.cupom(), cupons, "CUPOM_INVALIDO");
            if (!cupom.aplicavel(pedido.itens(), subtotal)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(pedido.itens(), subtotal, frete);
        }

        FormaPagamento formaPagamento = buscar(pedido.formaPagamento(), formasPagamento, "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        if (!formaPagamento.parcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = subtotal.multiply(regiao.getTaxaSeguro())
                .setScale(2, ARREDONDAMENTO);

        BigDecimal totalPedido = subtotal
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro)
                .setScale(2, ARREDONDAMENTO);

        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = resultado.totalFinal()
                .subtract(totalPedido)
                .setScale(2, ARREDONDAMENTO);

        BigDecimal credito = nivel.calcularCredito(subtotal);
        boolean brinde = nivel.brinde(subtotal);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento,
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                credito,
                brinde);
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null
                    || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null
                    || item.quantidade() <= 0
                    || item.pesoKg() == null
                    || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(
                    item.precoUnitario().multiply(new BigDecimal(item.quantidade())));
        }
        return subtotal.setScale(2, ARREDONDAMENTO);
    }

    private BigDecimal calcularPeso(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }

    private <T> T buscar(String codigo, Map<String, T> mapa, String codigoErro) {
        if (codigo == null || codigo.isBlank()) {
            throw new CheckoutException(codigoErro);
        }
        T valor = mapa.get(codigo);
        if (valor == null) {
            throw new CheckoutException(codigoErro);
        }
        return valor;
    }

    private Regiao buscarRegiao(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }
}
