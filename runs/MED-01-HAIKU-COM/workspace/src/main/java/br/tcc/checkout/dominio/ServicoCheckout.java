package br.tcc.checkout.dominio;

import br.tcc.checkout.dominio.cupom.Cupom;
import br.tcc.checkout.dominio.cupom.CupomFreteGratis;
import br.tcc.checkout.dominio.cupom.RepositorioCupons;
import br.tcc.checkout.dominio.entrega.ModalidadeEntrega;
import br.tcc.checkout.dominio.entrega.RepositorioModalidadesEntrega;
import br.tcc.checkout.dominio.pagamento.FormaPagamento;
import br.tcc.checkout.dominio.pagamento.RepositorioFormasPagamento;
import br.tcc.checkout.dominio.pagamento.ResultadoPagamento;
import br.tcc.checkout.dto.ItemPedido;
import br.tcc.checkout.dto.RequisicaoResumo;
import br.tcc.checkout.dto.RespostaResumo;
import br.tcc.checkout.exception.ErroCheckout;
import br.tcc.checkout.util.Arredondador;
import java.math.BigDecimal;
import java.util.List;

public class ServicoCheckout {
    private final RepositorioCupons repositorioCupons;
    private final RepositorioModalidadesEntrega repositorioModalidades;
    private final RepositorioFormasPagamento repositorioFormas;

    public ServicoCheckout(RepositorioCupons repositorioCupons,
                          RepositorioModalidadesEntrega repositorioModalidades,
                          RepositorioFormasPagamento repositorioFormas) {
        this.repositorioCupons = repositorioCupons;
        this.repositorioModalidades = repositorioModalidades;
        this.repositorioFormas = repositorioFormas;
    }

    public RespostaResumo calcularResumo(RequisicaoResumo requisicao) {
        List<ItemPedido> itens = requisicao.getItens();
        String modalidadeCodigo = requisicao.getModalidadeEntrega();
        String cupomCodigo = requisicao.getCupom();
        String formaCodigo = requisicao.getFormaPagamento();
        Integer parcelas = requisicao.getParcelas();

        validarPedido(itens);

        BigDecimal pesoTotalKg = calcularPesoTotal(itens);

        ModalidadeEntrega modalidade = obterModalidadeEntrega(modalidadeCodigo);
        validarDisponibilidadeModalidade(modalidade, pesoTotalKg);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);

        Cupom cupom = obterCupom(cupomCodigo, subtotalProdutos);

        BigDecimal descontoCupom = calcularDescontoCupom(cupom, subtotalProdutos, itens);

        BigDecimal freteCalculado = modalidade.calcularFrete(pesoTotalKg);

        if (cupom instanceof CupomFreteGratis) {
            ((CupomFreteGratis) cupom).setFreteCalculado(freteCalculado);
            descontoCupom = freteCalculado;
        }

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(freteCalculado);
        totalPedido = Arredondador.arredondarParaCentavos(totalPedido);

        FormaPagamento forma = obterFormaPagamento(formaCodigo);
        validarParcelamento(forma, parcelas, totalPedido);

        Integer parcelasAjustadas = parcelas != null ? parcelas : 1;
        ResultadoPagamento resultadoPagamento = forma.calcular(totalPedido, parcelasAjustadas);

        return new RespostaResumo(
            subtotalProdutos,
            descontoCupom,
            freteCalculado,
            modalidade.getPrazo(),
            resultadoPagamento.getAjuste(),
            resultadoPagamento.getTotalFinal(),
            parcelasAjustadas,
            resultadoPagamento.getValorParcela()
        );
    }

    private void validarPedido(List<ItemPedido> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO", "Carrinho vazio");
        }

        for (ItemPedido item : itens) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() <= 0) {
                throw new ErroCheckout("PEDIDO_INVALIDO", "Item inválido");
            }
        }
    }

    private BigDecimal calcularPesoTotal(List<ItemPedido> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal peso = new BigDecimal(item.getPesoKg().toString());
            peso = peso.multiply(new BigDecimal(item.getQuantidade()));
            pesoTotal = pesoTotal.add(peso);
        }
        return pesoTotal;
    }

    private ModalidadeEntrega obterModalidadeEntrega(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ErroCheckout("MODALIDADE_INVALIDA", "Modalidade não informada");
        }
        return repositorioModalidades.obter(codigo)
            .orElseThrow(() -> new ErroCheckout("MODALIDADE_INVALIDA", "Modalidade inexistente: " + codigo));
    }

    private void validarDisponibilidadeModalidade(ModalidadeEntrega modalidade, BigDecimal pesoTotalKg) {
        if (!modalidade.ehDisponivel(pesoTotalKg)) {
            throw new ErroCheckout("MODALIDADE_INDISPONIVEL", "Modalidade não disponível para este pedido");
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal preco = new BigDecimal(item.getPrecoUnitario().toString());
            BigDecimal quantidade = new BigDecimal(item.getQuantidade());
            subtotal = subtotal.add(preco.multiply(quantidade));
        }
        return Arredondador.arredondarParaCentavos(subtotal);
    }

    private Cupom obterCupom(String codigo, BigDecimal subtotalProdutos) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }

        return repositorioCupons.obter(codigo)
            .orElseThrow(() -> new ErroCheckout("CUPOM_INVALIDO", "Cupom inexistente: " + codigo));
    }

    private BigDecimal calcularDescontoCupom(Cupom cupom, BigDecimal subtotalProdutos, List<ItemPedido> itens) {
        if (cupom == null) {
            return new BigDecimal("0.00");
        }

        BigDecimal pesoTotalKg = calcularPesoTotal(itens);

        if (!cupom.ehAplicavel(subtotalProdutos, pesoTotalKg)) {
            throw new ErroCheckout("CUPOM_NAO_APLICAVEL", "Cupom não se aplica a este pedido");
        }

        return cupom.calcularDesconto(subtotalProdutos, itens);
    }

    private FormaPagamento obterFormaPagamento(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA", "Forma de pagamento não informada");
        }
        return repositorioFormas.obter(codigo)
            .orElseThrow(() -> new ErroCheckout("FORMA_PAGAMENTO_INVALIDA", "Forma de pagamento inexistente: " + codigo));
    }

    private void validarParcelamento(FormaPagamento forma, Integer parcelas, BigDecimal totalPedido) {
        if (!forma.ehValida(parcelas, totalPedido)) {
            String codigo = forma.getCodigo();
            if ("PIX".equals(codigo) || "BOLETO".equals(codigo)) {
                throw new ErroCheckout("PARCELAMENTO_INVALIDO", "Esta forma de pagamento não permite parcelamento");
            } else if ("CARTAO".equals(codigo)) {
                int p = parcelas != null ? parcelas : 1;
                if (p < 1 || p > 12) {
                    throw new ErroCheckout("PARCELAMENTO_INVALIDO", "Cartão permite de 1 a 12 parcelas");
                }
            }
            throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL", "Esta forma de pagamento não é permitida para este pedido");
        }
    }

    public static ServicoCheckout criar() {
        return new ServicoCheckout(
            RepositorioCupons.criar(),
            RepositorioModalidadesEntrega.criar(),
            RepositorioFormasPagamento.criar()
        );
    }
}
