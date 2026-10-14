package com.loja.pedidos.checkout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.loja.pedidos.clube.NivelClube;
import com.loja.pedidos.cupom.ContextoCupom;
import com.loja.pedidos.cupom.Cupom;
import com.loja.pedidos.cupom.LinhaItem;
import com.loja.pedidos.dominio.Dinheiro;
import com.loja.pedidos.dominio.ItemPedido;
import com.loja.pedidos.dominio.Produto;
import com.loja.pedidos.dominio.Regiao;
import com.loja.pedidos.dominio.Resumo;
import com.loja.pedidos.entrega.Cotacao;
import com.loja.pedidos.entrega.ModalidadeEntrega;
import com.loja.pedidos.entrega.PedidoEntrega;
import com.loja.pedidos.erro.CodigoErro;
import com.loja.pedidos.erro.ErroNegocio;
import com.loja.pedidos.pagamento.Cobranca;
import com.loja.pedidos.pagamento.FormaPagamento;
import com.loja.pedidos.persistencia.ProdutoRepositorio;

/** Único lugar onde o resumo é calculado: usado tanto pelo resumo quanto pelo registro do pedido. */
@Component
public class CalculadoraResumo {

    private static final Pattern CPF = Pattern.compile("\\d{11}");
    private static final Pattern CEP = Pattern.compile("\\d{8}");
    private static final BigDecimal TETO_DESCONTO = new BigDecimal("0.30");

    private final ProdutoRepositorio produtos;
    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, FormaPagamento> formasPagamento;
    private final Map<String, NivelClube> niveis;

    public CalculadoraResumo(ProdutoRepositorio produtos, List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
            List<FormaPagamento> formasPagamento, List<NivelClube> niveis) {
        this.produtos = produtos;
        this.modalidades = indexar(modalidades, ModalidadeEntrega::codigo);
        this.cupons = indexar(cupons, Cupom::codigo);
        this.formasPagamento = indexar(formasPagamento, FormaPagamento::codigo);
        this.niveis = indexar(niveis, NivelClube::codigo);
    }

    private static <T> Map<String, T> indexar(List<T> lista, Function<T, String> chave) {
        Map<String, T> mapa = new LinkedHashMap<>();
        lista.forEach(e -> mapa.put(chave.apply(e), e));
        return mapa;
    }

    public Resumo calcular(CompraRequest r) {
        // 1 a 4: carrinho
        validarBasico(r);
        List<String> skus = r.itens().stream().map(CompraRequest.ItemDados::sku).toList();
        Map<String, Produto> catalogo = produtos.buscarPorSkus(skus);
        if (!catalogo.keySet().containsAll(skus)) {
            throw new ErroNegocio(CodigoErro.PRODUTO_INEXISTENTE);
        }
        if (catalogo.values().stream().anyMatch(Produto::foraDeLinha)) {
            throw new ErroNegocio(CodigoErro.PRODUTO_FORA_DE_LINHA);
        }
        List<LinhaItem> linhas = new ArrayList<>();
        for (CompraRequest.ItemDados item : r.itens()) {
            Produto p = catalogo.get(item.sku());
            if (item.quantidade() > p.estoque()) {
                throw new ErroNegocio(CodigoErro.ESTOQUE_INSUFICIENTE);
            }
            linhas.add(new LinhaItem(p, item.quantidade()));
        }

        // 5 a 7: cliente e endereço
        NivelClube nivel = r.nivelClube() == null ? null : niveis.get(r.nivelClube());
        if (nivel == null) {
            throw new ErroNegocio(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
        Regiao regiao = Regiao.de(r.regiao());
        if (regiao == null) {
            throw new ErroNegocio(CodigoErro.REGIAO_INVALIDA);
        }
        if (r.cep() == null || !CEP.matcher(r.cep()).matches()) {
            throw new ErroNegocio(CodigoErro.CEP_INVALIDO);
        }

        // 8 a 10: entrega
        ModalidadeEntrega modalidade = r.modalidadeEntrega() == null ? null : modalidades.get(r.modalidadeEntrega());
        if (modalidade == null) {
            throw new ErroNegocio(CodigoErro.MODALIDADE_INVALIDA);
        }
        BigDecimal subtotal = linhas.stream().map(LinhaItem::total).reduce(Dinheiro.ZERO, BigDecimal::add);
        BigDecimal peso = linhas.stream()
                .map(l -> l.produto().pesoKg().multiply(BigDecimal.valueOf(l.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Cotacao cotacao = modalidade.cotar(new PedidoEntrega(r.cep(), regiao, peso));
        BigDecimal frete = nivel.isentaFrete() && modalidade.permiteIsencaoFrete() ? Dinheiro.ZERO : cotacao.frete();

        // 11 e 12: cupom
        BigDecimal desconto = Dinheiro.ZERO;
        if (r.cupom() != null) {
            Cupom cupom = cupons.get(r.cupom());
            if (cupom == null) {
                throw new ErroNegocio(CodigoErro.CUPOM_INVALIDO);
            }
            BigDecimal bruto = cupom.desconto(new ContextoCupom(r.cliente().cpf(), linhas, subtotal, modalidade, frete));
            desconto = Dinheiro.minimo(Dinheiro.arredondar(bruto), Dinheiro.fracao(subtotal, TETO_DESCONTO));
        }

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);

        // 13 a 15: pagamento
        FormaPagamento forma = r.formaPagamento() == null ? null : formasPagamento.get(r.formaPagamento());
        if (forma == null) {
            throw new ErroNegocio(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        int parcelas = r.parcelas() == null ? 1 : r.parcelas();
        forma.validarParcelas(parcelas);
        forma.validarDisponibilidade(total);
        Cobranca cobranca = forma.cobrar(total, parcelas, r.cupom() != null);

        return new Resumo(
                subtotal,
                desconto,
                frete,
                cotacao.prazoDias(),
                seguro,
                cobranca.totalFinal().subtract(total),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                Dinheiro.fracao(subtotal, nivel.fracaoCredito()),
                nivel.brinde(subtotal));
    }

    private void validarBasico(CompraRequest r) {
        if (r == null || r.cliente() == null || r.itens() == null || r.itens().isEmpty()) {
            throw new ErroNegocio(CodigoErro.PEDIDO_INVALIDO);
        }
        Set<String> vistos = new HashSet<>();
        for (CompraRequest.ItemDados item : r.itens()) {
            if (item == null || item.sku() == null || item.sku().isBlank() || !vistos.add(item.sku())
                    || item.quantidade() == null || item.quantidade() <= 0) {
                throw new ErroNegocio(CodigoErro.PEDIDO_INVALIDO);
            }
        }
        String cpf = r.cliente().cpf();
        String email = r.cliente().email();
        if (cpf == null || !CPF.matcher(cpf).matches() || email == null || email.isBlank()) {
            throw new ErroNegocio(CodigoErro.PEDIDO_INVALIDO);
        }
    }

    public static List<ItemPedido> itensDe(CompraRequest r) {
        return r.itens().stream().map(i -> new ItemPedido(i.sku(), i.quantidade())).toList();
    }
}
