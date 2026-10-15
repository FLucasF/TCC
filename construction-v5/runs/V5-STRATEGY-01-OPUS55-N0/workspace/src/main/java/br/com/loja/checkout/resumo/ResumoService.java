package br.com.loja.checkout.resumo;

import static br.com.loja.checkout.dominio.CodigoErro.*;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.ContextoCupom;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Catalogo;
import br.com.loja.checkout.dominio.CheckoutException;
import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Item;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.Cobranca;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.regiao.Regiao;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {

    private final Catalogo<NivelClube> niveis;
    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> formasPagamento;

    public ResumoService(List<NivelClube> niveis, List<ModalidadeEntrega> modalidades,
            List<Cupom> cupons, List<FormaPagamento> formasPagamento) {
        this.niveis = new Catalogo<>(niveis, NivelClube::codigo);
        this.modalidades = new Catalogo<>(modalidades, ModalidadeEntrega::codigo);
        this.cupons = new Catalogo<>(cupons, Cupom::codigo);
        this.formasPagamento = new Catalogo<>(formasPagamento, FormaPagamento::codigo);
    }

    /** Calcula o resumo, conferindo os problemas na ordem combinada com a loja. */
    public ResumoCompra calcular(SolicitacaoResumo solicitacao) {
        Carrinho carrinho = carrinho(solicitacao.itens());
        NivelClube nivel = exigir(niveis.buscar(solicitacao.nivelClube()), NIVEL_CLUBE_INVALIDO);
        Regiao regiao = exigir(Regiao.buscar(solicitacao.regiao()), REGIAO_INVALIDA);
        ModalidadeEntrega modalidade = exigir(modalidades.buscar(solicitacao.modalidadeEntrega()), MODALIDADE_INVALIDA);
        conferir(modalidade.atende(carrinho), MODALIDADE_INDISPONIVEL);

        BigDecimal subtotal = carrinho.subtotalProdutos();
        BigDecimal frete = nivel.isentaFrete() ? Dinheiro.ZERO : modalidade.frete(carrinho);

        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (solicitacao.cupom() != null) {
            Cupom cupom = exigir(cupons.buscar(solicitacao.cupom()), CUPOM_INVALIDO);
            ContextoCupom contexto = new ContextoCupom(carrinho, subtotal, frete);
            conferir(cupom.aplicavel(contexto), CUPOM_NAO_APLICAVEL);
            descontoCupom = Dinheiro.centavos(cupom.desconto(contexto));
        }

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        FormaPagamento formaPagamento = exigir(formasPagamento.buscar(solicitacao.formaPagamento()),
                FORMA_PAGAMENTO_INVALIDA);
        int parcelas = parcelas(solicitacao.parcelas());
        conferir(formaPagamento.parcelamentoPermitido(parcelas), PARCELAMENTO_INVALIDO);
        conferir(formaPagamento.disponivel(totalPedido), FORMA_PAGAMENTO_INDISPONIVEL);

        Cobranca cobranca = formaPagamento.cobrar(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.centavos(cobranca.totalFinal());

        return new ResumoCompra(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                totalFinal.subtract(totalPedido),
                totalFinal,
                parcelas,
                Dinheiro.centavos(cobranca.valorParcela()),
                Dinheiro.percentual(subtotal, nivel.percentualCredito()),
                nivel.ganhaBrinde(subtotal));
    }

    private static Carrinho carrinho(List<ItemSolicitado> itensSolicitados) {
        conferir(itensSolicitados != null && !itensSolicitados.isEmpty(), PEDIDO_INVALIDO);
        List<Item> itens = new ArrayList<>();
        for (ItemSolicitado item : itensSolicitados) {
            conferir(item != null && positivo(item.precoUnitario()) && positivo(item.pesoKg())
                    && inteiroPositivo(item.quantidade()), PEDIDO_INVALIDO);
            itens.add(new Item(item.nome(), item.precoUnitario(), item.quantidade().intValueExact(), item.pesoKg()));
        }
        return new Carrinho(itens);
    }

    private static int parcelas(BigDecimal parcelas) {
        if (parcelas == null) {
            return 1;
        }
        conferir(inteiroPositivo(parcelas), PARCELAMENTO_INVALIDO);
        return parcelas.intValueExact();
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static boolean inteiroPositivo(BigDecimal valor) {
        if (!positivo(valor)) {
            return false;
        }
        try {
            valor.intValueExact();
            return true;
        } catch (ArithmeticException e) {
            return false;
        }
    }

    private static <T> T exigir(Optional<T> valor, CodigoErro erro) {
        return valor.orElseThrow(() -> new CheckoutException(erro));
    }

    private static void conferir(boolean condicao, CodigoErro erro) {
        if (!condicao) {
            throw new CheckoutException(erro);
        }
    }
}
