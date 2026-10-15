package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.SemCupom;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Erro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/** Monta o resumo da compra que o site mostra antes de o cliente confirmar. */
@Service
public class CalculadoraResumo {

    private static final int PARCELAS_PADRAO = 1;

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveisClube;
    private final Catalogo<FormaPagamento> formasPagamento;
    private final Catalogo<Regiao> regioes;

    public CalculadoraResumo(Catalogo<ModalidadeEntrega> modalidades,
                             Catalogo<Cupom> cupons,
                             Catalogo<NivelClube> niveisClube,
                             Catalogo<FormaPagamento> formasPagamento,
                             Catalogo<Regiao> regioes) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
        this.regioes = regioes;
    }

    public ResumoResponse calcular(ResumoRequest dados) {
        Erro.PEDIDO_INVALIDO.recusarSe(dados == null);

        Pedido pedido = conferirPedido(dados.itens());
        NivelClube nivel = niveisClube.buscar(dados.nivelClube());
        Regiao regiao = regioes.buscar(dados.regiao());

        ModalidadeEntrega modalidade = modalidades.buscar(dados.modalidadeEntrega());
        Erro.MODALIDADE_INDISPONIVEL.recusarSe(!modalidade.atende(pedido));

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = nivel.isentaFrete() ? Dinheiro.ZERO : modalidade.frete(pedido);

        Cupom cupom = escolherCupom(dados.cupom());
        ContextoCupom contexto = new ContextoCupom(pedido, frete);
        Erro.CUPOM_NAO_APLICAVEL.recusarSe(!cupom.aplicavel(contexto));
        BigDecimal descontoCupom = cupom.desconto(contexto);

        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento = formasPagamento.buscar(dados.formaPagamento());
        int parcelas = dados.parcelas() == null ? PARCELAS_PADRAO : dados.parcelas();
        Erro.PARCELAMENTO_INVALIDO.recusarSe(!formaPagamento.aceitaParcelas(parcelas));
        Erro.FORMA_PAGAMENTO_INDISPONIVEL.recusarSe(!formaPagamento.atende(totalPedido));
        ResultadoPagamento pagamento = formaPagamento.liquidar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                Dinheiro.arredondar(pagamento.totalFinal().subtract(totalPedido)),
                Dinheiro.arredondar(pagamento.totalFinal()),
                parcelas,
                Dinheiro.arredondar(pagamento.valorParcela()),
                nivel.creditoProximaCompra(subtotalProdutos),
                nivel.ganhaBrinde(subtotalProdutos));
    }

    private Cupom escolherCupom(String codigo) {
        return codigo == null ? SemCupom.INSTANCIA : cupons.buscar(codigo);
    }

    private Pedido conferirPedido(List<ResumoRequest.ItemRequest> itens) {
        Erro.PEDIDO_INVALIDO.recusarSe(itens == null || itens.isEmpty());
        return new Pedido(itens.stream().map(this::conferirItem).toList());
    }

    private ItemPedido conferirItem(ResumoRequest.ItemRequest item) {
        Erro.PEDIDO_INVALIDO.recusarSe(item == null
                || naoEPositivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || naoEPositivo(item.pesoKg()));
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean naoEPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }
}
