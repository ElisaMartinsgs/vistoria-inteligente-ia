# 5. Relato do Processo e Engenharia de Prompt

> Entrega Parcial GrowUp · Item 5 (2,0 pts)

## 5.1 Repositório de prompts (Prompt Ops)

Os prompts de produção ficam em `backend/src/main/resources/prompts/`, versionados no Git. Cada chamada grava a versão usada em `chamada_ia.versao_prompt`, então dá para comparar resultados entre versões.

### P1 — `extracao-item-v1` (rascunho, substituído)

| Campo | Conteúdo |
|---|---|
| **Objetivo** | Transformar o texto do Tesseract em JSON com os campos do item de vistoria |
| **Contexto informado** | Só o texto do OCR |
| **Prompt** | "Extraia as informações do documento abaixo. Retorne apenas os campos: tipoItem, dataValidade, classificacaoRisco, normaAssociada. Se não encontrar um campo, retorne null. Não invente informações. TEXTO: {{textoTesseract}}" |
| **Saída esperada** | `{"tipoItem": "...", "dataValidade": "...", "classificacaoRisco": "...", "normaAssociada": "..."}` |
| **Problemas identificados na revisão do prompt** | (1) não restringe os valores: a IA pode devolver "Extintor de incêndio", "Fogo", "nr23" — difícil de comparar em Java; (2) não força JSON: modelos costumam responder com texto antes ("Claro! Aqui está…") ou dentro de ```json; (3) não diz o que fazer com datas parciais: a IA pode converter "08/2027" para "2027-08-01", escolhendo o dia por conta própria; (4) não há como saber quando a IA está "chutando"; (5) faltam campos para as regras (item presente, selo, condição visual) |

### P2 — `extracao-item-v2` (em produção)

Arquivo: [`prompts/extracao-item-v2.txt`](../backend/src/main/resources/prompts/extracao-item-v2.txt)

| Campo | Conteúdo |
|---|---|
| **Objetivo** | Extrair **todos** os campos que as regras usam, em formato fechado, sem inventar, com autoavaliação de confiança |
| **Contexto informado** | *Sistema:* papel (assistente de vistoria em restaurantes no Brasil), 10 regras, lista de valores válidos de `tipoItem` e `classificacaoRisco` (gerada a partir dos *enums* Java, sempre sincronizada), valores de `condicaoVisual`, exemplo do JSON exato. *Usuário:* item que o técnico diz estar fotografando, local, unidade do cadastro, data de hoje e o texto do OCR entre `"""` |
| **Saída esperada** | JSON com 14 campos: `tipoItem, unidade, enderecoUnidade, tecnicoResponsavel, dataVistoria, itemPresente, condicaoVisual, seloValidadePresente, dataValidade, localizacao, classificacaoRisco, normaAssociada, grauUrgencia, confianca` — ausentes como `null` |
| **Parâmetros** | `temperature: 0`, `response_format: json_object`, OCR truncado em 4.000 caracteres |
| **O que mudou e por quê** | Valores fechados (resolve 1); `response_format` + parser tolerante (resolve 2); "copie as datas exatamente como aparecem, não calcule" — o cálculo é do Java (resolve 3); campo `confianca` com regra "< 0,7 se precisou adivinhar" (resolve 4); novos campos (resolve 5); regra 9 sobre erros típicos de OCR; regra 10 contra *prompt injection* |
| **Problemas ainda em aberto** | A confiança é autoavaliada (não é calibrada); por isso também usamos regras de coerência no Java. A regra 7 permite deduzir a norma pelo tipo do item; isso não afeta a decisão, porque o Java confere a norma na tabela de NRs. **Falta medir com chamadas reais** ao modelo escolhido e com fotos reais (ver US19) |
| **Como testamos** | `PromptBuilderTest` (template sem placeholders sobrando, truncamento) e `LlmResponseParserTest` (respostas problemáticas típicas de LLM: markdown, snake_case, "sim", confiança 0–100, JSON cortado). Próximo passo: rodar o conjunto golden contra o modelo real e registrar os resultados aqui |

### P3 — Persona para entrevista com agente

| Campo | Conteúdo |
|---|---|
| **Objetivo** | Levantar dores e casos de borda antes da entrevista humana |
| **Contexto** | "Você é uma técnica de segurança do trabalho com 8 anos de experiência em redes de restaurantes no Brasil. Responda de forma prática, com exemplos do dia a dia, e diga quando não souber." + roteiro do doc 1 |
| **Saída esperada** | Respostas curtas por pergunta |
| **Problemas** | Respostas genéricas e "certinhas demais"; não refletem a Sabor & Cia. Usado só para gerar hipóteses (ver doc 1) |

### P4 — Geração de casos de teste

| Campo | Conteúdo |
|---|---|
| **Objetivo** | Listar formatos de data e respostas de IA problemáticas para virar teste |
| **Contexto** | Código do `DataParser` e do `LlmResponseParser` |
| **Saída esperada** | Lista de entradas + resultado esperado |
| **Problemas** | Cada caso sugerido precisa ser revisado antes de virar teste, porque o resultado esperado é uma decisão do projeto (ex.: a regra é **data impossível → revisão humana**, não "corrigir" a data) |

## 5.2 Relato do processo

### O que foi feito até agora

1. **Entendimento do problema** a partir de um caso real (extintor com foto anexada, mas validade não reconhecida; prazo de recarga estourando) e do material da mentoria (Encontro 2: entender → definir → desenhar → MVP).
2. **Definição** do documento (foto do item/selo ou PDF do checklist), dos dados a extrair, das regras de aprovação/reprovação e do fluxo OpenCV → Tesseract → IA → Spring Boot → PostgreSQL.
3. **Implementação do MVP completo:**
   - API REST com 12 endpoints, tratamento de erros padronizado (RFC 7807);
   - pipeline OpenCV (perspectiva, rotação, CLAHE, ruído, binarização, métricas de legibilidade);
   - OCR com Tess4J em português, com timeout;
   - integração com LLM por interface, com *retry*, *fallback* por Regex e auditoria de cada chamada;
   - motor com 10 regras independentes e mensagens em linguagem simples;
   - banco com 6 tabelas e migrações Flyway;
   - front-end mobile-first com 4 telas (vistoria, histórico, revisão do supervisor, painel);
   - Docker Compose e CI no GitHub.
4. **Demonstração preparada** com 3 imagens (conforme, vencido, ilegível) e um teste automático que roda os 3 cenários com o texto real lido pelo Tesseract.

### Como trabalhamos

- O fluxo do infográfico da mentoria virou a estrutura de pacotes (`infra/imagem`, `infra/ocr`, `infra/ia`, `dominio/regras`, `persistencia`).
- **Regras primeiro, com testes**: cada regra do documento de definição virou uma classe e pelo menos um teste antes de ligar a IA.
- **A IA entrou por último e atrás de uma interface**, para que o sistema funcionasse (modo `regex`) mesmo sem chave de API.

### Impacto das ferramentas de IA no processo

- **Velocidade:** o assistente de código gerou o esqueleto do Spring Boot, as entidades, os DTOs e as primeiras versões das regras em pouco tempo, liberando tempo para pensar nos casos de borda.
- **Revisão:** pedir ao assistente para "procurar o que pode quebrar" levantou casos que viraram regras (prompt injection na etiqueta, etiqueta de outra unidade, data absurda).
- **Mas nada foi aceito sem verificar.** Problemas reais encontrados ao validar o que a IA produziu:

| Problema encontrado | Como descobrimos | Ajuste |
|---|---|---|
| `Comparator.comparingDouble(Imgproc::contourArea).reversed()` não compilava (inferência de tipo genérico + método sobrecarregado) | Compilação | Tipo explícito: `Comparator.<MatOfPoint>comparingDouble(...)` |
| Acesso a `vistoria.getUnidade().getId()` fora de transação podia dar `LazyInitializationException` | Revisão de código | Consulta dentro de `TransactionTemplate` |
| O Tesseract lia a borda da etiqueta como `\|` no fim da linha ("Boa Viagem \|") | Rodando OCR nas imagens de exemplo | Limpeza no `RegexExtractor` + comparação de unidade só com letras e números |
| A faixa vermelha com texto branco sumia na binarização | Imagem tratada salva para inspeção | Aceito: as informações importantes estão no corpo da etiqueta; documentado |
| A primeira versão do prompt deixava em aberto o tratamento de datas parciais | Revisão do prompt v1 | v2: "copie a data como aparece"; cálculo em Java (`DataParser`) |
| Imagem Docker com Ubuntu 22.04 traria Tesseract 4, incompatível com Tess4J 5 | Leitura da documentação das versões | Imagem base Ubuntu 24.04 (Tesseract 5) + `libtesseract-dev` (o JNA procura `libtesseract.so`) |
| `minAreaRect` mudou a convenção de ângulo no OpenCV 4.5+ | Documentação do OpenCV | Normalização do ângulo para −45°…45° e limite de 15° |
| Limiares de legibilidade "chutados" | Medição real: foto boa ≈ 280 de nitidez; foto tremida ≈ 0,5 | Limites calibrados: nitidez 40, brilho 45–240, contraste 18 |

### O que aprendemos

- OCR **não entende** o documento; ele só devolve letras. Entender é papel da IA, e **decidir** é papel do código, onde dá para testar.
- A melhor proteção contra alucinação não é só o prompt: é o desenho do sistema (campos `null`, regras de coerência, limite de confiança, revisão humana).
- Tratar a imagem antes do OCR faz diferença: com perspectiva corrigida, CLAHE e binarização, a confiança do OCR passou de 90% nas imagens de teste.
- Falha de API externa é **normal**, não exceção: o sistema precisa de um plano B (fallback) e de registro (tabela `chamada_ia`).

### Dificuldades

- Configurar Tesseract/OpenCV em máquinas diferentes (resolvido com Docker).
- Não ter fotos reais suficientes para medir a acurácia: as imagens de exemplo são simuladas. **Próximo passo:** coletar 20–30 fotos reais de etiquetas (US19 do backlog).
- Equilibrar rigor e utilidade: reprovar demais irrita o técnico; aprovar demais é perigoso. A saída foi o terceiro status (REVISAO_MANUAL).

> ✍️ **Para a equipe completar:** acrescentem aqui a experiência pessoal de vocês (o que cada um fez, o que foi mais difícil, o que fariam diferente). Essa parte precisa ser escrita por vocês.
