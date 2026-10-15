package com.loja.checkout.servico;

import com.loja.checkout.dominio.ErroPedido;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.clube.Niveis;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.Cupons;
import com.loja.checkout.dominio.cupom.SemCupom;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.entrega.Modalidades;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.Pagamentos;
import com.loja.checkout.web.ResumoRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ValidadorPedido {

    public Pedido validar(ResumoRequest req) {
        List<Item> itens = validarItens(req);
        NivelClube nivel = Niveis.buscar(req.nivelClube)
                .orElseThrow(() -> new ErroPedido("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Regiao.buscar(req.regiao)
                .orElseThrow(() -> new ErroPedido("REGIAO_INVALIDA"));
        ModalidadeEntrega modalidade = Modalidades.buscar(req.modalidadeEntrega)
                .orElseThrow(() -> new ErroPedido("MODALIDADE_INVALIDA"));

        BigDecimal pesoTotal = itens.stream().map(Item::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!modalidade.atende(pesoTotal)) {
            throw new ErroPedido("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = resolverCupom(req, itens);

        FormaPagamento forma = Pagamentos.buscar(req.formaPagamento)
                .orElseThrow(() -> new ErroPedido("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = req.parcelas == null ? 1 : req.parcelas;
        if (!forma.parcelasValidas(parcelas)) {
            throw new ErroPedido("PARCELAMENTO_INVALIDO");
        }

        return new Pedido(itens, modalidade, cupom, forma, parcelas, nivel, regiao);
    }

    private List<Item> validarItens(ResumoRequest req) {
        if (req.itens == null || req.itens.isEmpty()) {
            throw new ErroPedido("PEDIDO_INVALIDO");
        }
        List<Item> itens = new ArrayList<>();
        for (ResumoRequest.ItemRequest i : req.itens) {
            if (i == null
                    || i.precoUnitario == null || i.precoUnitario.signum() <= 0
                    || i.quantidade == null || i.quantidade <= 0
                    || i.pesoKg == null || i.pesoKg.signum() <= 0) {
                throw new ErroPedido("PEDIDO_INVALIDO");
            }
            itens.add(new Item(i.nome, i.precoUnitario, i.quantidade, i.pesoKg));
        }
        return itens;
    }

    private Cupom resolverCupom(ResumoRequest req, List<Item> itens) {
        if (req.cupom == null) {
            return new SemCupom();
        }
        Cupom cupom = Cupons.buscar(req.cupom)
                .orElseThrow(() -> new ErroPedido("CUPOM_INVALIDO"));
        BigDecimal subtotal = itens.stream().map(Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!cupom.aplicavel(itens, subtotal)) {
            throw new ErroPedido("CUPOM_NAO_APLICAVEL");
        }
        return cupom;
    }
}
