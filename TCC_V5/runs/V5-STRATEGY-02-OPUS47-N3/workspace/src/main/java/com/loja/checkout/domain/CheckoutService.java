package com.loja.checkout.domain;

import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.domain.regiao.Regiao;
import com.loja.checkout.web.dto.CheckoutRequest;
import com.loja.checkout.web.dto.ItemRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CheckoutService {

    public Resumo calcular(CheckoutRequest req) {
        List<Item> itens = validarItens(req.itens);
        NivelClube nivel = parseEnum(req.nivelClube, NivelClube.class, CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = parseEnum(req.regiao, Regiao.class, CodigoErro.REGIAO_INVALIDA);
        ModalidadeEntrega modalidade = parseEnum(req.modalidadeEntrega, ModalidadeEntrega.class, CodigoErro.MODALIDADE_INVALIDA);

        BigDecimal pesoTotal = pesoTotal(itens);
        if (!modalidade.atende(pesoTotal)) {
            throw new PedidoInvalidoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = Dinheiro.ajustar(subtotal(itens));
        BigDecimal freteBase = modalidade.frete(pesoTotal);
        BigDecimal frete = Dinheiro.ajustar(nivel.fretePago(freteBase));

        Cupom cupom = null;
        if (req.cupom != null && !req.cupom.isEmpty()) {
            cupom = parseEnum(req.cupom, Cupom.class, CodigoErro.CUPOM_INVALIDO);
        }
        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (cupom != null) {
            Cupom.ContextoCupom ctx = new Cupom.ContextoCupom(subtotalProdutos, frete, itens);
            if (!cupom.aplicavel(ctx)) {
                throw new PedidoInvalidoException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = Dinheiro.ajustar(cupom.desconto(ctx));
        }

        FormaPagamento forma = parseEnum(req.formaPagamento, FormaPagamento.class, CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = req.parcelas == null ? 1 : req.parcelas;
        if (!forma.parcelasValidas(parcelas)) {
            throw new PedidoInvalidoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal seguro = Dinheiro.ajustar(subtotalProdutos.multiply(regiao.aliquotaSeguro()));
        BigDecimal totalPedido = Dinheiro.ajustar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!forma.disponivel(totalPedido)) {
            throw new PedidoInvalidoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento pagamento = forma.calcular(totalPedido, parcelas);
        BigDecimal credito = nivel.credito(subtotalProdutos);
        boolean brinde = nivel.temBrinde(subtotalProdutos);

        return new Resumo(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.ajuste(),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                credito,
                brinde);
    }

    private static List<Item> validarItens(List<ItemRequest> brutos) {
        if (brutos == null || brutos.isEmpty()) {
            throw new PedidoInvalidoException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<Item> itens = new ArrayList<>(brutos.size());
        for (ItemRequest r : brutos) {
            if (r == null
                    || r.precoUnitario == null || r.precoUnitario.signum() <= 0
                    || r.quantidade == null || r.quantidade <= 0
                    || r.pesoKg == null || r.pesoKg.signum() <= 0) {
                throw new PedidoInvalidoException(CodigoErro.PEDIDO_INVALIDO);
            }
            itens.add(new Item(r.nome, r.precoUnitario, r.quantidade, r.pesoKg));
        }
        return itens;
    }

    private static BigDecimal subtotal(List<Item> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item i : itens) {
            total = total.add(i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())));
        }
        return total;
    }

    private static BigDecimal pesoTotal(List<Item> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item i : itens) {
            total = total.add(i.pesoKg().multiply(BigDecimal.valueOf(i.quantidade())));
        }
        return total;
    }

    private static <E extends Enum<E>> E parseEnum(String nome, Class<E> tipo, CodigoErro erro) {
        if (nome == null) throw new PedidoInvalidoException(erro);
        try {
            return Enum.valueOf(tipo, nome);
        } catch (IllegalArgumentException e) {
            throw new PedidoInvalidoException(erro);
        }
    }
}
