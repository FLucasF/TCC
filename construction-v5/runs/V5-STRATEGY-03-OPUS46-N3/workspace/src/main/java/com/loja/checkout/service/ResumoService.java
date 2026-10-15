package com.loja.checkout.service;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.Regiao;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
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
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> formasPagamento;

    public ResumoService(List<ModalidadeEntrega> modalidades,
                         List<Cupom> cupons,
                         List<NivelClube> niveis,
                         List<FormaPagamento> formasPagamento) {
        this.modalidades = modalidades.stream().collect(Collectors.toMap(ModalidadeEntrega::getCodigo, Function.identity()));
        this.cupons = cupons.stream().collect(Collectors.toMap(Cupom::getCodigo, Function.identity()));
        this.niveis = niveis.stream().collect(Collectors.toMap(NivelClube::getCodigo, Function.identity()));
        this.formasPagamento = formasPagamento.stream().collect(Collectors.toMap(FormaPagamento::getCodigo, Function.identity()));
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        validarItens(pedido.itens());

        NivelClube nivel = niveis.get(pedido.nivelClube());
        if (nivel == null) throw new CheckoutException("NIVEL_CLUBE_INVALIDO");

        Regiao regiao = Regiao.porCodigo(pedido.regiao());
        if (regiao == null) throw new CheckoutException("REGIAO_INVALIDA");

        ModalidadeEntrega modalidade = modalidades.get(pedido.modalidadeEntrega());
        if (modalidade == null) throw new CheckoutException("MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = calcularPeso(pedido.itens());
        if (!modalidade.isDisponivel(pesoTotal)) throw new CheckoutException("MODALIDADE_INDISPONIVEL");

        Cupom cupom = null;
        if (pedido.cupom() != null && !pedido.cupom().isEmpty()) {
            cupom = cupons.get(pedido.cupom());
            if (cupom == null) throw new CheckoutException("CUPOM_INVALIDO");
        }

        BigDecimal subtotal = calcularSubtotal(pedido.itens());

        if (cupom != null && !cupom.isAplicavel(subtotal, pedido.itens())) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        FormaPagamento forma = formasPagamento.get(pedido.formaPagamento());
        if (forma == null) throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");

        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        if (!forma.isParcelamentoValido(parcelas)) throw new CheckoutException("PARCELAMENTO_INVALIDO");

        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        if (nivel.isFreteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            descontoCupom = cupom.calcularDesconto(subtotal, pedido.itens(), frete);
        }

        BigDecimal seguro = subtotal.multiply(regiao.getTaxaSeguro()).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!forma.isDisponivel(totalPedido)) throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");

        ResultadoPagamento resultado = forma.calcular(totalPedido, parcelas);

        BigDecimal credito = nivel.calcularCredito(subtotal);
        boolean brinde = nivel.temBrinde(subtotal);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.getPrazoDias(),
                seguro,
                resultado.ajuste(),
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela(),
                credito,
                brinde
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) throw new CheckoutException("PEDIDO_INVALIDO");
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0)
                throw new CheckoutException("PEDIDO_INVALIDO");
            if (item.quantidade() == null || item.quantidade() <= 0)
                throw new CheckoutException("PEDIDO_INVALIDO");
            if (item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0)
                throw new CheckoutException("PEDIDO_INVALIDO");
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPeso(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }
}
