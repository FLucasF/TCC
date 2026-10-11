package com.loja.checkout.calculo;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.clube.Niveis;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.Cupons;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.Modalidades;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormasPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.regiao.Regiao;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResumoService {

    public CheckoutResponse calcular(CheckoutRequest req) {
        List<ItemCarrinho> itens = validarItens(req);
        NivelClube nivel = Niveis.por(req.nivelClube)
                .orElseThrow(() -> new ErroPedido("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Regiao.por(req.regiao)
                .orElseThrow(() -> new ErroPedido("REGIAO_INVALIDA"));
        ModalidadeEntrega modalidade = Modalidades.por(req.modalidadeEntrega)
                .orElseThrow(() -> new ErroPedido("MODALIDADE_INVALIDA"));
        if (!modalidade.atende(itens)) {
            throw new ErroPedido("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = null;
        if (req.cupom != null) {
            if (!Cupons.existe(req.cupom)) throw new ErroPedido("CUPOM_INVALIDO");
            cupom = Cupons.por(req.cupom).orElseThrow();
        }

        FormaPagamento pagamento = FormasPagamento.por(req.formaPagamento)
                .orElseThrow(() -> new ErroPedido("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = req.parcelas == null ? 1 : req.parcelas;
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new ErroPedido("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotal = Dinheiro.cents(
                itens.stream().map(ItemCarrinho::subtotalItem).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal frete = nivel.frete(modalidade.frete(itens));

        if (cupom != null && !cupom.aplicavel(subtotal, frete, itens)) {
            throw new ErroPedido("CUPOM_NAO_APLICAVEL");
        }
        BigDecimal descontoCupom = cupom == null ? Dinheiro.zero() : cupom.desconto(subtotal, frete, itens);

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = Dinheiro.cents(
                subtotal.subtract(descontoCupom).add(frete).add(seguro));

        if (!pagamento.disponivel(totalPedido)) {
            throw new ErroPedido("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        ResultadoPagamento rp = pagamento.calcular(totalPedido, parcelas);

        CheckoutResponse resp = new CheckoutResponse();
        resp.subtotalProdutos = subtotal;
        resp.descontoCupom = descontoCupom;
        resp.frete = frete;
        resp.prazoEntregaDias = modalidade.prazoDias();
        resp.seguro = seguro;
        resp.ajustePagamento = rp.ajuste();
        resp.totalFinal = rp.totalFinal();
        resp.parcelas = rp.parcelas();
        resp.valorParcela = rp.valorParcela();
        resp.creditoProximaCompra = nivel.creditoProximaCompra(subtotal);
        resp.brinde = nivel.brinde(subtotal);
        return resp;
    }

    private List<ItemCarrinho> validarItens(CheckoutRequest req) {
        if (req.itens == null || req.itens.isEmpty()) {
            throw new ErroPedido("PEDIDO_INVALIDO");
        }
        List<ItemCarrinho> itens = new ArrayList<>(req.itens.size());
        for (CheckoutRequest.Item i : req.itens) {
            if (i == null || i.precoUnitario == null || i.quantidade == null || i.pesoKg == null) {
                throw new ErroPedido("PEDIDO_INVALIDO");
            }
            if (i.precoUnitario.signum() <= 0 || i.quantidade <= 0 || i.pesoKg.signum() <= 0) {
                throw new ErroPedido("PEDIDO_INVALIDO");
            }
            itens.add(new ItemCarrinho(i.nome, i.precoUnitario, i.quantidade, i.pesoKg));
        }
        return itens;
    }
}
