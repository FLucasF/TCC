package br.tcc.checkout.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import br.tcc.checkout.dto.ItemCarrinho;
import br.tcc.checkout.dto.RequisicaoResumo;
import br.tcc.checkout.dto.RespostaResumo;
import br.tcc.checkout.exception.ErroCheckout;
import br.tcc.checkout.model.Cupom;
import br.tcc.checkout.model.FormaPagamento;
import br.tcc.checkout.model.ModalidadeEntrega;
import br.tcc.checkout.util.Arredondamento;

@Service
public class CheckoutService {

    public RespostaResumo calcularResumo(RequisicaoResumo requisicao) throws ErroCheckout {
        // Validação 1: Pedido inválido
        validarPedido(requisicao);

        // Validação 2: Modalidade não existe ou não informada
        validarModalidadeExiste(requisicao.getModalidadeEntrega());

        // Validação 3: Modalidade indisponível
        validarModalidadeDisponivel(requisicao.getItens(), requisicao.getModalidadeEntrega());

        // Validação 4: Cupom inválido
        Cupom cupom = null;
        if (requisicao.getCupom() != null && !requisicao.getCupom().isEmpty()) {
            cupom = Cupom.porCodigo(requisicao.getCupom());
            if (cupom == null) {
                throw new ErroCheckout("CUPOM_INVALIDO");
            }
        }

        // Validação 5: Cupom não aplicável
        if (cupom != null) {
            validarCupomAplicavel(requisicao.getItens(), cupom);
        }

        // Validação 6: Forma de pagamento não existe ou não informada
        validarFormaPagamentoExiste(requisicao.getFormaPagamento());

        // Validação 7: Número de parcelas não permitido
        Integer parcelas = requisicao.getParcelas() != null ? requisicao.getParcelas() : 1;
        validarParcelamento(requisicao.getFormaPagamento(), parcelas);

        // Calcula valores antes de validar forma de pagamento
        BigDecimal subtotalProdutos = calcularSubtotalProdutos(requisicao.getItens());
        BigDecimal descontoCupom = calcularDescontoCupom(requisicao.getItens(), cupom, subtotalProdutos);
        ModalidadeEntrega modalidade = ModalidadeEntrega.porCodigo(requisicao.getModalidadeEntrega());
        BigDecimal frete = calcularFrete(requisicao.getItens(), modalidade);

        // Total antes de ajuste de pagamento
        BigDecimal totalAntesAjuste = Arredondamento.arredondar(
            subtotalProdutos.subtract(descontoCupom).add(frete)
        );

        // Validação 8: Forma de pagamento indisponível
        validarFormaPagamentoDisponivel(requisicao.getFormaPagamento(), totalAntesAjuste);

        // Calcula ajuste de pagamento
        BigDecimal ajustePagamento = calcularAjustePagamento(
            requisicao.getFormaPagamento(), totalAntesAjuste, parcelas
        );

        BigDecimal totalFinal = Arredondamento.arredondar(totalAntesAjuste.add(ajustePagamento));
        BigDecimal valorParcela = calcularValorParcela(totalFinal, parcelas,
            requisicao.getFormaPagamento());

        return new RespostaResumo(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.getPrazo(),
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela
        );
    }

    private void validarPedido(RequisicaoResumo requisicao) throws ErroCheckout {
        List<ItemCarrinho> itens = requisicao.getItens();

        if (itens == null || itens.isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : itens) {
            if (item.getNome() == null || item.getNome().isEmpty()) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
            if (item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarModalidadeExiste(String codigoModalidade) throws ErroCheckout {
        if (codigoModalidade == null || codigoModalidade.isEmpty()) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }
        if (ModalidadeEntrega.porCodigo(codigoModalidade) == null) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }
    }

    private void validarModalidadeDisponivel(List<ItemCarrinho> itens, String codigoModalidade) throws ErroCheckout {
        ModalidadeEntrega modalidade = ModalidadeEntrega.porCodigo(codigoModalidade);

        if (modalidade == ModalidadeEntrega.MOTOBOY) {
            BigDecimal pesoTotal = itens.stream()
                .map(item -> item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (pesoTotal.compareTo(new BigDecimal("5.00")) > 0) {
                throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
            }
        }
    }

    private void validarCupomAplicavel(List<ItemCarrinho> itens, Cupom cupom) throws ErroCheckout {
        BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);

        if (cupom.getMinimoSubtotal().compareTo(BigDecimal.ZERO) > 0) {
            if (subtotalProdutos.compareTo(cupom.getMinimoSubtotal()) < 0) {
                throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
            }
        }
    }

    private void validarFormaPagamentoExiste(String codigoForma) throws ErroCheckout {
        if (codigoForma == null || codigoForma.isEmpty()) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }
        if (FormaPagamento.porCodigo(codigoForma) == null) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarParcelamento(String codigoForma, Integer parcelas) throws ErroCheckout {
        FormaPagamento forma = FormaPagamento.porCodigo(codigoForma);

        if (forma == FormaPagamento.PIX || forma == FormaPagamento.BOLETO) {
            if (parcelas != 1) {
                throw new ErroCheckout("PARCELAMENTO_INVALIDO");
            }
        } else if (forma == FormaPagamento.CARTAO) {
            if (parcelas < 1 || parcelas > 12) {
                throw new ErroCheckout("PARCELAMENTO_INVALIDO");
            }
        }
    }

    private void validarFormaPagamentoDisponivel(String codigoForma, BigDecimal total) throws ErroCheckout {
        FormaPagamento forma = FormaPagamento.porCodigo(codigoForma);

        if (forma == FormaPagamento.BOLETO) {
            if (total.compareTo(new BigDecimal("1000.00")) > 0) {
                throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal subtotal = itens.stream()
            .map(item -> item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Arredondamento.arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(List<ItemCarrinho> itens, Cupom cupom, BigDecimal subtotalProdutos) {
        if (cupom == null) {
            return Arredondamento.arredondar(BigDecimal.ZERO);
        }

        return switch (cupom.getTipo()) {
            case "PERCENTUAL" -> {
                BigDecimal desconto = subtotalProdutos.multiply(cupom.getValor());
                yield Arredondamento.arredondar(desconto);
            }
            case "FIXO" -> Arredondamento.arredondar(cupom.getValor());
            case "FRETE_GRATIS" -> Arredondamento.arredondar(BigDecimal.ZERO);
            case "LEVE3PAGUE2" -> calcularDescontoLeve3Pague2(itens);
            default -> Arredondamento.arredondar(BigDecimal.ZERO);
        };
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemCarrinho> itens) {
        BigDecimal totalDesconto = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            int quantidade = item.getQuantidade();
            int gratis = quantidade / 3;
            if (gratis > 0) {
                BigDecimal desconto = item.getPrecoUnitario().multiply(new BigDecimal(gratis));
                totalDesconto = totalDesconto.add(desconto);
            }
        }

        return Arredondamento.arredondar(totalDesconto);
    }

    private BigDecimal calcularFrete(List<ItemCarrinho> itens, ModalidadeEntrega modalidade) {
        if (modalidade == ModalidadeEntrega.RETIRADA_LOJA) {
            return Arredondamento.arredondar(BigDecimal.ZERO);
        }

        if (modalidade == ModalidadeEntrega.MOTOBOY) {
            return Arredondamento.arredondar(modalidade.getTaxaBase());
        }

        BigDecimal pesoTotal = itens.stream()
            .map(item -> item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal frete = modalidade.getTaxaBase()
            .add(modalidade.getTaxaPorKg().multiply(pesoTotal));

        return Arredondamento.arredondar(frete);
    }

    private BigDecimal calcularAjustePagamento(String codigoForma, BigDecimal total, Integer parcelas) {
        FormaPagamento forma = FormaPagamento.porCodigo(codigoForma);

        return switch (forma) {
            case PIX -> {
                BigDecimal desconto = total.multiply(new BigDecimal("0.05"));
                yield Arredondamento.arredondar(desconto).negate();
            }
            case CARTAO -> calcularAjusteCartao(total, parcelas);
            case BOLETO -> Arredondamento.arredondar(new BigDecimal("3.49"));
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal calcularAjusteCartao(BigDecimal total, Integer parcelas) {
        if (parcelas <= 3) {
            return Arredondamento.arredondar(BigDecimal.ZERO);
        }

        BigDecimal taxaMensal = new BigDecimal("0.0199");
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxaMensal);
        BigDecimal taxa = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(taxa, 10, java.math.RoundingMode.HALF_EVEN));

        BigDecimal parcela = total.multiply(taxaMensal).divide(denominador,
            10, java.math.RoundingMode.HALF_EVEN);
        parcela = Arredondamento.arredondar(parcela);

        BigDecimal totalComJuros = parcela.multiply(new BigDecimal(parcelas));
        BigDecimal juros = totalComJuros.subtract(total);

        return Arredondamento.arredondar(juros);
    }

    private BigDecimal calcularValorParcela(BigDecimal totalFinal, Integer parcelas, String codigoForma) {
        BigDecimal parcela = totalFinal.divide(new BigDecimal(parcelas),
            10, java.math.RoundingMode.HALF_EVEN);
        return Arredondamento.arredondar(parcela);
    }
}
