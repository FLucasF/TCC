package com.loja.checkout.dominio;

import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.clube.Niveis;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.Cupons;
import com.loja.checkout.dominio.entrega.Modalidade;
import com.loja.checkout.dominio.entrega.Modalidades;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.Pagamento;
import com.loja.checkout.dominio.pagamento.Pagamentos;
import com.loja.checkout.web.PedidoRequest;
import com.loja.checkout.web.PedidoRequest.ItemRequest;
import com.loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResumoService {

    public ResumoResponse calcular(PedidoRequest req) {
        List<Item> itens = validarItens(req.itens());
        NivelClube nivel = Niveis.resolver(req.nivelClube());
        Regiao regiao = Regiao.resolver(req.regiao());

        Modalidade modalidade = Modalidades.resolver(req.modalidadeEntrega());
        BigDecimal pesoKg = somaPeso(itens);
        if (!modalidade.atende(pesoKg)) throw new ErroPedido("MODALIDADE_INDISPONIVEL");

        BigDecimal subtotal = Dinheiro.arredondar(somaSubtotal(itens));
        BigDecimal frete = nivel.isentaFrete() ? Dinheiro.ZERO : modalidade.custo(pesoKg);

        Cupom cupom = req.cupom() == null ? null : Cupons.resolver(req.cupom());
        if (cupom != null && !cupom.aplicavel(itens, subtotal, frete))
            throw new ErroPedido("CUPOM_NAO_APLICAVEL");
        BigDecimal descontoCupom = cupom == null
            ? Dinheiro.ZERO
            : Dinheiro.arredondar(cupom.desconto(itens, subtotal, frete));

        FormaPagamento forma = Pagamentos.resolver(req.formaPagamento());
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.parcelasValidas(parcelas)) throw new ErroPedido("PARCELAMENTO_INVALIDO");

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!forma.atende(totalPedido, parcelas)) throw new ErroPedido("FORMA_PAGAMENTO_INDISPONIVEL");
        Pagamento pag = forma.calcular(totalPedido, parcelas);
        BigDecimal ajuste = Dinheiro.arredondar(pag.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
            subtotal,
            descontoCupom,
            frete,
            modalidade.prazoDias(),
            seguro,
            ajuste,
            pag.totalFinal(),
            parcelas,
            pag.valorParcela(),
            nivel.credito(subtotal),
            nivel.ganhaBrinde(subtotal)
        );
    }

    private List<Item> validarItens(List<ItemRequest> reqs) {
        if (reqs == null || reqs.isEmpty()) throw new ErroPedido("PEDIDO_INVALIDO");
        List<Item> out = new ArrayList<>(reqs.size());
        for (ItemRequest r : reqs) {
            if (r == null) throw new ErroPedido("PEDIDO_INVALIDO");
            if (r.precoUnitario() == null || r.quantidade() == null || r.pesoKg() == null)
                throw new ErroPedido("PEDIDO_INVALIDO");
            if (r.precoUnitario().signum() <= 0 || r.quantidade() <= 0 || r.pesoKg().signum() <= 0)
                throw new ErroPedido("PEDIDO_INVALIDO");
            out.add(new Item(r.precoUnitario(), r.quantidade(), r.pesoKg()));
        }
        return out;
    }

    private BigDecimal somaSubtotal(List<Item> itens) {
        return itens.stream().map(Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somaPeso(List<Item> itens) {
        return itens.stream().map(Item::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
