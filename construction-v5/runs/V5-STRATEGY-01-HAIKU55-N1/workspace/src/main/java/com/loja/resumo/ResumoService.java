package com.loja.resumo;

import com.loja.resumo.ResumoRequisicao.ItemRequisicao;
import com.loja.resumo.clube.NivelClube;
import com.loja.resumo.cupom.Cupom;
import com.loja.resumo.entrega.OpcaoEntrega;
import com.loja.resumo.pagamento.FormaPagamento;
import com.loja.resumo.pagamento.Pagamento;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {

    private final Catalogo<OpcaoEntrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> formasPagamento;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<Regiao> regioes;

    public ResumoService(
            Catalogo<OpcaoEntrega> entregas,
            Catalogo<Cupom> cupons,
            Catalogo<FormaPagamento> formasPagamento,
            Catalogo<NivelClube> niveis,
            Catalogo<Regiao> regioes) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.formasPagamento = formasPagamento;
        this.niveis = niveis;
        this.regioes = regioes;
    }

    public ResumoResposta calcular(ResumoRequisicao requisicao) {
        List<ItemPedido> itens = validarItens(requisicao.itens());
        NivelClube nivel = niveis.buscar(requisicao.nivelClube())
                .orElseThrow(() -> new ErroResumo("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = regioes.buscar(requisicao.regiao())
                .orElseThrow(() -> new ErroResumo("REGIAO_INVALIDA"));
        OpcaoEntrega entrega = entregas.buscar(requisicao.modalidadeEntrega())
                .orElseThrow(() -> new ErroResumo("MODALIDADE_INVALIDA"));

        BigDecimal peso = itens.stream().map(ItemPedido::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!entrega.atende(peso)) {
            throw new ErroResumo("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = Dinheiro.arredondar(itens.stream()
                .map(ItemPedido::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal frete = nivel.freteGratis()
                ? Dinheiro.ZERO
                : Dinheiro.arredondar(entrega.frete(peso));

        Contexto contexto = new Contexto(itens, subtotal, frete);
        BigDecimal desconto = aplicarCupom(requisicao.cupom(), contexto);

        BigDecimal seguro = Dinheiro.arredondar(subtotal.multiply(regiao.taxaSeguro()));
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);

        FormaPagamento forma = formasPagamento.buscar(requisicao.formaPagamento())
                .orElseThrow(() -> new ErroResumo("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        if (parcelas < 1 || parcelas > forma.maxParcelas()) {
            throw new ErroResumo("PARCELAMENTO_INVALIDO");
        }
        forma.verificarDisponivel(total);
        Pagamento pagamento = forma.aplicar(total, parcelas);

        return new ResumoResposta(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                Dinheiro.arredondar(pagamento.totalFinal().subtract(total)),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                Dinheiro.arredondar(subtotal.multiply(nivel.percentualCredito())),
                nivel.temBrinde(subtotal));
    }

    private List<ItemPedido> validarItens(List<ItemRequisicao> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroResumo("PEDIDO_INVALIDO");
        }
        return itens.stream().map(this::validarItem).toList();
    }

    private ItemPedido validarItem(ItemRequisicao item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !positivo(item.pesoKg())) {
            throw new ErroResumo("PEDIDO_INVALIDO");
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private BigDecimal aplicarCupom(String codigo, Contexto contexto) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo).orElseThrow(() -> new ErroResumo("CUPOM_INVALIDO"));
        cupom.verificarAplicavel(contexto);
        return Dinheiro.arredondar(cupom.desconto(contexto));
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }
}
