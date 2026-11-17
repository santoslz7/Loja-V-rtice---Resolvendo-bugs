# Vértice - Depuração de Loja Virtual

Atividade de depuração: loja virtual de acessórios de tecnologia (Java + HTML/CSS/JS) com cinco incidentes reportados pelo suporte. O objetivo foi reproduzir, investigar e corrigir cada defeito com a menor alteração segura, sem quebrar funcionalidades existentes.

## Tecnologias

- Java 17+ (servidor HTTP nativo, sem dependências externas)
- HTML, CSS e JavaScript (front-end em `web/`)
- Dados em memória (reiniciar o servidor restaura o estado inicial)

## Como executar

1. Tenha o JDK 17 ou superior instalado (`javac -version`).
2. Na pasta do projeto:
   - Windows: `executar.bat`
   - Linux/macOS: `sh executar.sh`
3. Acesse http://localhost:8765
4. Para encerrar, pressione `Ctrl+C`.

## Estrutura

```
src/loja/
    Main.java           -> servidor HTTP, rotas e respostas
    LojaService.java    -> catálogo, carrinho e pedidos
    PrecoService.java   -> desconto e frete
    Produto.java        -> dados de produto
    Pedido.java         -> dados de pedido
    ErroNegocio.java    -> exceções de regra de negócio
    Json.java           -> formatação JSON
web/
    index.html, styles.css, app.js
```

## Incidentes corrigidos

| Chamado | Problema | Causa | Correção |
|---|---|---|---|
| INC-001 | Carrinho aceitava mais unidades que o estoque | Validação comparava só a quantidade pedida, não o total acumulado | Comparar `quantidadeNova` (atual + pedida) com o estoque em `LojaService.adicionarAoCarrinho` |
| INC-002 | Desconto do cupom `BEMVINDO10` incorreto | Divisão por 100 (1%) em vez de 10 (10%) | Ajuste do cálculo em `PrecoService.desconto` |
| INC-003 | Carrinho na tela desatualizado | `refreshCart()` só rodava quando o produto era novo no carrinho | Atualizar o carrinho após toda adição bem-sucedida em `app.js` |
| INC-004 | Produto inexistente causava erro 500 | `buscarProduto` retornava `null` e a rota chamava `.toJson()` | `buscarProduto` passou a usar `produtoOuErro`, retornando 404 |
| INC-005 | Pedido cancelado aparecia em "em aberto" | Filtro de `listarPedidos` só excluía pedidos entregues | Filtro passou a excluir também os cancelados |

## Como investigamos

- **Navegador (F12 > Network):** observação de rota, método, status HTTP, payload e resposta de cada requisição.
- **Leitura do código:** do sintoma (tela/Network) até o método responsável no Java ou no `app.js`.
- **Retestes:** cada correção foi verificada no cenário defeituoso e em um cenário válido, para garantir que nada quebrou.

O registro detalhado de cada incidente (reprodução, evidência, causa, correção e verificação) está em `RELATORIO_MODELO.md`.

## Observações

Projeto local de fins didáticos, sem autenticação, pagamentos reais ou dados de clientes. Não é destinado a produção.