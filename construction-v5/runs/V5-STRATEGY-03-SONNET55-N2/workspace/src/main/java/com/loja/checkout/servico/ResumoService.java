package com.loja.checkout.servico;

import static com.loja.checkout.dominio.CodigoErro.*;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroNegocio;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.Cobranca;
import com.loja.checkout.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {

    private final Catalogo<NivelClube> niveis;
    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> formasPagamento;

    public ResumoService(List<NivelClube> niveis, List<ModalidadeEntrega> modalidades,
                         List<Cupom> cupons, List<FormaPagamento> formasPagamento) {
        this.niveis = new Catalogo<>(niveis);
        this.modalidades = new Catalogo<>(modalidades);
        this.cupons = new Catalogo<>(cupons);
        this.formasPagamento = new Catalogo<>(formasPagamento);
    }

    public ResumoResponse calcular(ResumoRequest req) {
        Pedido pedido = montarPedido(req);
        NivelClube nivel = niveis.buscar(req.nivelClube()).orElseThrow(() -> new ErroNegocio(NIVEL_CLUBE_INVALIDO));
        Regiao regiao = parseRegiao(req.regiao());
        ModalidadeEntrega modalidade = modalidades.buscar(req.modalidadeEntrega())
                .orElseThrow(() -> new ErroNegocio(MODALIDADE_INVALIDA));
        if (!modalidade.atende(pedido)) {
            throw new ErroNegocio(MODALIDADE_INDISPONIVEL);
        }
        Cupom cupom = null;
        if (req.cupom() != null) {
            cupom = cupons.buscar(req.cupom()).orElseThrow(() -> new ErroNegocio(CUPOM_INVALIDO));
            if (!cupom.aplicavel(pedido)) {
                throw new ErroNegocio(CUPOM_NAO_APLICAVEL);
            }
        }
        FormaPagamento forma = formasPagamento.buscar(req.formaPagamento())
                .orElseThrow(() -> new ErroNegocio(FORMA_PAGAMENTO_INVALIDA));
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.aceitaParcelas(parcelas)) {
            throw new ErroNegocio(PARCELAMENTO_INVALIDO);
        }

        BigDecimal subtotal = pedido.subtotalProdutos();
        BigDecimal frete = nivel.frete(modalidade.frete(pedido));
        BigDecimal desconto = cupom == null ? Dinheiro.ZERO : cupom.desconto(pedido, frete);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        if (!forma.disponivel(totalPedido)) {
            throw new ErroNegocio(FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Cobranca cobranca = forma.cobrar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(pedido),
                seguro,
                cobranca.totalFinal().subtract(totalPedido),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                nivel.credito(pedido),
                nivel.brinde(pedido));
    }

    private Pedido montarPedido(ResumoRequest req) {
        if (req.itens() == null || req.itens().isEmpty()) {
            throw new ErroNegocio(PEDIDO_INVALIDO);
        }
        List<Item> itens = req.itens().stream().map(this::montarItem).toList();
        return new Pedido(itens);
    }

    private Item montarItem(ResumoRequest.ItemRequest i) {
        if (i == null
                || i.precoUnitario() == null || i.precoUnitario().signum() <= 0
                || i.quantidade() == null || i.quantidade() <= 0
                || i.pesoKg() == null || i.pesoKg().signum() <= 0) {
            throw new ErroNegocio(PEDIDO_INVALIDO);
        }
        return new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg());
    }

    private Regiao parseRegiao(String valor) {
        try {
            return Regiao.valueOf(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ErroNegocio(REGIAO_INVALIDA);
        }
    }
}
