package com.loja.checkout;

import com.loja.checkout.ResumoRequest.ItemRequest;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> formasPagamento;

    public CheckoutService(List<ModalidadeEntrega> modalidades,
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

    public ResumoResponse calcular(ResumoRequest request) {
        validarItens(request.itens());

        NivelClube nivel = encontrar(niveis, request.nivelClube(), "NIVEL_CLUBE_INVALIDO");

        Regiao regiao = encontrarRegiao(request.regiao());

        ModalidadeEntrega modalidade = encontrar(modalidades, request.modalidadeEntrega(),
                "MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = calcularPesoTotal(request.itens());
        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = calcularSubtotal(request.itens());

        Cupom cupom = null;
        if (request.cupom() != null && !request.cupom().isBlank()) {
            cupom = encontrar(cupons, request.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(request.itens(), subtotal)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        FormaPagamento formaPagamento = encontrar(formasPagamento, request.formaPagamento(),
                "FORMA_PAGAMENTO_INVALIDA");
        if (!formaPagamento.parcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        if (nivel.freteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            descontoCupom = cupom.calcularDesconto(request.itens(), subtotal, frete);
        }

        BigDecimal seguro = Moeda.arredondar(subtotal.multiply(regiao.getTaxaSeguro()));

        BigDecimal totalPedido = Moeda.arredondar(
                subtotal.subtract(descontoCupom).add(frete).add(seguro));

        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal ajuste = Moeda.arredondar(resultado.totalFinal().subtract(totalPedido));
        BigDecimal credito = nivel.calcularCredito(subtotal);
        boolean brinde = nivel.brinde(subtotal);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
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
        return Moeda.arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }

    private Regiao encontrarRegiao(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private <T> T encontrar(Map<String, T> mapa, String codigo, String codigoErro) {
        if (codigo == null || codigo.isBlank()) {
            throw new CheckoutException(codigoErro);
        }
        T resultado = mapa.get(codigo);
        if (resultado == null) {
            throw new CheckoutException(codigoErro);
        }
        return resultado;
    }
}
