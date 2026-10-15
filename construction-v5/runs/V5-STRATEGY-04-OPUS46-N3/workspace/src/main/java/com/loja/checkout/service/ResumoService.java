package com.loja.checkout.service;

import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ResumoService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> formasPagamento;

    public ResumoService(List<ModalidadeEntrega> modalidades,
                         List<Cupom> cupons,
                         List<NivelClube> niveis,
                         List<FormaPagamento> formasPagamento) {
        this.modalidades = modalidades.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
        this.cupons = cupons.stream()
                .collect(Collectors.toMap(Cupom::codigo, Function.identity()));
        this.niveis = niveis.stream()
                .collect(Collectors.toMap(NivelClube::codigo, Function.identity()));
        this.formasPagamento = formasPagamento.stream()
                .collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        validarItens(pedido.itens());

        BigDecimal subtotal = calcularSubtotal(pedido.itens());
        BigDecimal pesoTotal = calcularPesoTotal(pedido.itens());

        NivelClube nivel = buscar(niveis, pedido.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        buscarRegiao(pedido.regiao());
        Regiao regiao = Regiao.valueOf(pedido.regiao());

        ModalidadeEntrega entrega = buscar(modalidades, pedido.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        if (!entrega.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal frete = nivel.freteGratis()
                ? ZERO
                : entrega.calcularFrete(pesoTotal);

        BigDecimal descontoCupom = ZERO;
        if (pedido.cupom() != null && !pedido.cupom().isEmpty()) {
            Cupom cupom = buscar(cupons, pedido.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(pedido.itens(), subtotal)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(pedido.itens(), subtotal, frete);
        }

        FormaPagamento pagamento = buscar(formasPagamento, pedido.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = arredondar(subtotal.multiply(regiao.getPercentualSeguro()));
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!pagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido);
        BigDecimal credito = arredondar(subtotal.multiply(nivel.percentualCashback()));
        boolean brinde = nivel.temBrinde(subtotal);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                ajuste,
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                credito,
                brinde
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
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
            subtotal = subtotal.add(
                    item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private void buscarRegiao(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private <T> T buscar(Map<String, T> mapa, String codigo, String codigoErro) {
        if (codigo == null || codigo.isEmpty()) {
            throw new CheckoutException(codigoErro);
        }
        T resultado = mapa.get(codigo);
        if (resultado == null) {
            throw new CheckoutException(codigoErro);
        }
        return resultado;
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
