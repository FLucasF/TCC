package com.loja.checkout.service;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomContexto;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.PedidoException;
import com.loja.checkout.pagamento.AjustePagamento;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.regiao.Regiao;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CalculadoraResumoService {

    public ResumoResponse calcular(ResumoRequest request) {
        validarItens(request.itens());
        NivelClube nivelClube = NivelClube.fromCodigo(request.nivelClube())
                .orElseThrow(() -> new PedidoException(CodigoErro.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.fromCodigo(request.regiao())
                .orElseThrow(() -> new PedidoException(CodigoErro.REGIAO_INVALIDA));
        ModalidadeEntrega modalidade = ModalidadeEntrega.fromCodigo(request.modalidadeEntrega())
                .orElseThrow(() -> new PedidoException(CodigoErro.MODALIDADE_INVALIDA));

        BigDecimal subtotal = calcularSubtotal(request.itens());
        BigDecimal pesoTotal = calcularPesoTotal(request.itens());

        if (!modalidade.disponivel(pesoTotal)) {
            throw new PedidoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal frete = nivelClube.isentaFrete()
                ? new BigDecimal("0.00")
                : modalidade.calcularFrete(pesoTotal);

        BigDecimal descontoCupom = calcularDescontoCupom(request.cupom(), subtotal, frete, request.itens());

        FormaPagamento formaPagamento = FormaPagamento.fromCodigo(request.formaPagamento())
                .orElseThrow(() -> new PedidoException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelaValida(parcelas)) {
            throw new PedidoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal seguro = Dinheiro.arredondar(subtotal.multiply(regiao.percentualSeguro()));
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!formaPagamento.disponivel(totalPedido)) {
            throw new PedidoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        AjustePagamento ajustePagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal credito = nivelClube.calcularCredito(subtotal);
        boolean brinde = nivelClube.temBrinde(subtotal);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento.ajuste(),
                ajustePagamento.totalFinal(),
                parcelas,
                ajustePagamento.valorParcela(),
                credito,
                brinde);
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoException(CodigoErro.PEDIDO_INVALIDO);
        }
        for (ItemRequest item : itens) {
            boolean precoInvalido = item.precoUnitario() == null || item.precoUnitario().signum() <= 0;
            boolean quantidadeInvalida = item.quantidade() == null || item.quantidade() <= 0;
            boolean pesoInvalido = item.pesoKg() == null || item.pesoKg().signum() <= 0;
            if (precoInvalido || quantidadeInvalida || pesoInvalido) {
                throw new PedidoException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return Dinheiro.arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private BigDecimal calcularDescontoCupom(String codigoCupom, BigDecimal subtotal, BigDecimal frete,
            List<ItemRequest> itens) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return new BigDecimal("0.00");
        }
        Cupom cupom = Cupom.fromCodigo(codigoCupom)
                .orElseThrow(() -> new PedidoException(CodigoErro.CUPOM_INVALIDO));
        CupomContexto contexto = new CupomContexto(subtotal, frete, itens);
        if (!cupom.aplicavel(contexto)) {
            throw new PedidoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return cupom.calcularDesconto(contexto);
    }
}
