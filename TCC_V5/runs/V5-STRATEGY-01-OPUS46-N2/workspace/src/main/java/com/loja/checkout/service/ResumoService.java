package com.loja.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.loja.checkout.Regiao;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveisClube;
    private final Map<String, FormaPagamento> formasPagamento;

    public ResumoService(List<ModalidadeEntrega> modalidades,
                         List<Cupom> cupons,
                         List<NivelClube> niveisClube,
                         List<FormaPagamento> formasPagamento) {
        this.modalidades = modalidades.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::getCodigo, Function.identity()));
        this.cupons = cupons.stream()
                .collect(Collectors.toMap(Cupom::getCodigo, Function.identity()));
        this.niveisClube = niveisClube.stream()
                .collect(Collectors.toMap(NivelClube::getCodigo, Function.identity()));
        this.formasPagamento = formasPagamento.stream()
                .collect(Collectors.toMap(FormaPagamento::getCodigo, Function.identity()));
    }

    public ResumoResponse calcular(ResumoRequest request) {
        validarItens(request.itens());

        NivelClube nivel = niveisClube.get(request.nivelClube());
        if (nivel == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        Regiao regiao = Regiao.porCodigo(request.regiao());
        if (regiao == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        ModalidadeEntrega modalidade = modalidades.get(request.modalidadeEntrega());
        if (modalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        BigDecimal pesoTotal = calcularPesoTotal(request.itens());
        if (!modalidade.isDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = calcularSubtotal(request.itens());

        BigDecimal freteBruto = modalidade.calcularFrete(pesoTotal);
        BigDecimal frete = nivel.isFreteGratis() ? BigDecimal.ZERO.setScale(2) : freteBruto;

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (request.cupom() != null) {
            Cupom cupom = cupons.get(request.cupom());
            if (cupom == null) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
            if (!cupom.isAplicavel(subtotal, request.itens(), frete)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(subtotal, request.itens(), frete);
        }

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;

        FormaPagamento forma = formasPagamento.get(request.formaPagamento());
        if (forma == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        if (!forma.isParcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = subtotal.multiply(regiao.getPercentualSeguro())
                .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!forma.isDisponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = forma.calcular(totalPedido, parcelas);
        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.getPrazoDias(),
                seguro,
                ajuste,
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela(),
                nivel.calcularCredito(subtotal),
                nivel.temBrinde(subtotal)
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
