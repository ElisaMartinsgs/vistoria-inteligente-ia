# 2. Backlog e Engenharia de Requisitos "AI-First"

> Entrega Parcial GrowUp · Item 2 (2,0 pts)

Priorização **MoSCoW** (Must / Should / Could / Won't agora). Estimativa em pontos (1, 2, 3, 5, 8).
Status: ✅ feito no repositório · 🔜 próximo · 💡 evolução futura.

## 2.1 Épicos

| Épico | Objetivo | Prioridade |
|---|---|---|
| **E1 – Captura da evidência** | Técnico envia foto/PDF do item pelo celular | Must |
| **E2 – Pipeline de leitura** | OpenCV → Tesseract → IA → JSON estruturado | Must |
| **E3 – Regras de conformidade** | Decidir CONFORME / NÃO CONFORME / REVISÃO com motivo | Must |
| **E4 – Persistência e consulta** | Histórico de vistorias e evidências no PostgreSQL | Must |
| **E5 – Revisão humana** | Supervisor decide casos duvidosos | Should |
| **E6 – Painel de compliance** | Indicadores por unidade e motivos de reprovação | Should |
| **E7 – Qualidade e operação** | Testes, Docker, CI, observabilidade da IA | Must |
| **E8 – Evolução** | Notificação ao fornecedor, novos documentos, login | Could / Won't |

## 2.2 Histórias de usuário, tarefas e critérios de aceite

### E1 – Captura da evidência

**US01 – Fotografar item no checklist** · Must · 3 pts · ✅
*Como técnica de segurança, quero tirar a foto do item pelo celular e anexá-la à vistoria da unidade, para não precisar digitar nada.*
- Tarefas: tela mobile-first com seleção de unidade, item e local ✅ · `input capture=environment` ✅ · pré-visualização ✅ · agrupar itens na mesma vistoria (`vistoriaId`) ✅ · lembrar nome do técnico no aparelho ✅
- Critérios de aceite:
  - Dado que estou na tela Vistoria, quando escolho a foto e toco em "Validar item", então vejo o progresso das etapas e o parecer em até 15 s.
  - Arquivo > 10 MB ou formato diferente de JPG/PNG/PDF → mensagem clara, sem enviar.

**US02 – Enviar PDF de checklist escaneado** · Should · 2 pts · ✅
- Tarefas: converter a 1ª página do PDF em imagem a 300 DPI (PDFBox) ✅
- Aceite: PDF de 1 página é processado pelo mesmo pipeline das fotos; PDF corrompido/protegido → HTTP 400 com mensagem.

### E2 – Pipeline de leitura

**US03 – Tratar a imagem antes da leitura** · Must · 5 pts · ✅
*Como sistema, quero corrigir rotação, perspectiva, contraste e ruído, para o OCR ler melhor.*
- Tarefas: padronizar largura (1200 px) ✅ · perspectiva (maior quadrilátero) ✅ · deskew até 15° ✅ · CLAHE + remoção de ruído + limiar adaptativo ✅ · métricas nitidez/brilho/contraste ✅
- Aceite: nas imagens de `exemplos/`, a etiqueta é recortada e o OCR atinge confiança ≥ 90%.

**US04 – Extrair o texto da etiqueta (OCR)** · Must · 3 pts · ✅
- Tarefas: Tess4J em português ✅ · confiança média por linha ✅ · pool limitado + timeout ✅
- Aceite: texto e confiança são salvos no item; falha do OCR não derruba a API (item vai para revisão).

**US05 – Estruturar os dados com IA** · Must · 8 pts · ✅
*Como sistema, quero transformar o texto do OCR em JSON com os campos da vistoria, para aplicar as regras.*
- Tarefas: prompt versionado em arquivo ✅ · cliente compatível com OpenAI/Gemini/Groq/Ollama ✅ · `response_format=json_object` + temperatura 0 ✅ · parser tolerante ✅ · fallback por Regex ✅ · auditoria em `chamada_ia` ✅
- **Critérios de aceite de IA** (ver 2.4): JSON válido em 100% das respostas aceitas; campo ausente = `null`; nenhuma data inventada no conjunto de teste; falha da IA nunca gera HTTP 500.

**US06 – Modo sem IA** · Should · 2 pts · ✅
- Aceite: com `IA_PROVEDOR=regex` o sistema funciona sem chave e sem custo; etiquetas de layout conhecido são aprovadas.

**US07 – Modo multimodal** · Could · 2 pts · ✅
- Aceite: com `LLM_ENVIAR_IMAGEM=true` a foto vai junto ao texto do OCR; útil para "item presente?" e "condição visual".

### E3 – Regras de conformidade

**US08 – Validar campos e validade** · Must · 5 pts · ✅
*Como gerente, quero saber se o item está dentro da validade e o que fazer se não estiver.*
- Tarefas: `RegraCamposObrigatorios`, `RegraValidade` (Regex + `LocalDate`), alerta 30 dias, `RegraSeloValidade` ✅
- Aceite: validade 08/2027 → CONFORME; 10/08/2026 em 19/11/2026 → NÃO CONFORME "vencido há 101 dias"; 05/12/2026 → CONFORME com alerta "vence em 16 dias".

**US09 – Validar risco e norma** · Must · 3 pts · ✅
- Aceite: extintor + risco "escorregamento" → revisão; norma revogada (NR-27) → NÃO CONFORME.

**US10 – Reprovar foto ilegível ou de outro objeto** · Must · 3 pts · ✅
- Aceite: nitidez < 40 ou brilho < 45 → NÃO CONFORME "envie outra foto", sem chamar OCR/IA; foto de piso quando o item é extintor → NÃO CONFORME.

**US11 – Mensagem em linguagem simples** · Must · 2 pts · ✅
- Aceite: todo motivo tem `mensagemSimples` (para o técnico/gerente) e `detalheTecnico` (para o supervisor).

### E4 – Persistência e consulta

**US12 – Salvar resultado e evidência** · Must · 3 pts · ✅ — Flyway, 6 tabelas, arquivo original em disco/volume.
**US13 – Histórico de vistorias por unidade** · Should · 2 pts · ✅ — `GET /api/vistorias?unidadeId=`.

### E5 – Revisão humana

**US14 – Fila de revisão do supervisor** · Should · 3 pts · ✅
*Como supervisor, quero ver a foto e os motivos dos casos duvidosos e decidir, para não reprovar injustamente.*
- Aceite: `PATCH /api/itens/{id}/revisao` muda o status final, guarda quem/quando/por quê e **preserva o parecer automático** (`status_automatico`); revisar duas vezes → HTTP 422.

### E6 – Painel

**US15 – Indicadores de conformidade** · Should · 3 pts · ✅ — total, taxa, em revisão, top 5 motivos, por unidade.

### E7 – Qualidade e operação

**US16 – Testes automatizados das regras e da IA** · Must · 3 pts · ✅ — 36 testes JUnit (regras, datas, parser do LLM, Regex, prompt, cenários reais da demo).
**US17 – Subir tudo com um comando** · Must · 2 pts · ✅ — `docker compose up --build`.
**US18 – CI no GitHub** · Should · 1 pt · ✅ — GitHub Actions roda os testes a cada push.
**US19 – Conjunto "golden" de imagens reais** · Must · 3 pts · 🔜 — 20–30 fotos reais rotuladas para medir a acurácia da IA (ver 2.4).

### E8 – Evolução

**US20 – Notificar o fornecedor quando o extintor vencer** · Could · 5 pts · 💡 (dor do caso real)
**US21 – Novos documentos (laudo de dedetização, certificado do extintor, ficha de EPI)** · Could · 5 pts · 💡
**US22 – Login e perfis (técnico, gerente, supervisor)** · Should (pós-MVP) · 5 pts · 💡
**US23 – Armazenamento em nuvem (S3/Blob) e fila assíncrona** · Won't agora · 8 pts · 💡

## 2.3 Padrões de uso de ferramentas de IA no projeto

**No produto**

| Padrão | Como aplicamos |
|---|---|
| IA como *extrator*, não como *juiz* | O LLM só devolve dados; quem aprova/reprova é o `MotorDeRegras` em Java |
| Saída estruturada | `response_format: json_object`, temperatura 0, schema descrito no prompt, parser tolerante |
| "Não invente" explícito | Regra nº 1 do prompt + campos ausentes = `null` + regras que reprovam/revisam quando falta dado |
| Confiança autoavaliada | Campo `confianca` (0–1); abaixo de 0,7 → revisão humana |
| Humano no circuito (*human-in-the-loop*) | Status REVISAO_MANUAL + fila do supervisor |
| Degradação graciosa | Retry com backoff → fallback por Regex → revisão |
| Prompt versionado | `prompts/extracao-item-v2.txt`, versão gravada em cada `chamada_ia` |
| Observabilidade | Tokens, latência, tentativas, erro e resposta bruta salvos em `chamada_ia` |
| Provedor trocável | Interface `LlmClient`; qualquer API compatível com OpenAI por variável de ambiente |
| Minimizar dados enviados | Só o texto do OCR (máx. 4.000 caracteres); a foto só vai se o modo multimodal estiver ligado |

**No desenvolvimento**

| Ferramenta | Uso | Cuidado adotado |
|---|---|---|
| Assistente de código (Claude) | Gerar esqueleto, regras, testes, revisão de código | Todo código passou por compilação e testes; nada foi aceito sem rodar |
| LLM para personas | Entrevista simulada (ver doc 1) | Não conta como validação; só gera hipóteses |
| LLM para gerar casos de teste | Datas estranhas, respostas de IA quebradas, prompt injection | Casos viraram testes JUnit determinísticos |

## 2.4 Como garantir que a IA está funcionando (critérios de aceite de IA)

| Critério | Meta | Como medimos |
|---|---|---|
| JSON utilizável | 100% das respostas aceitas | `LlmResponseParserTest`; respostas inválidas viram fallback (contadas em `chamada_ia.sucesso=false`) |
| Não inventar dados | 0 datas inventadas no conjunto golden | Imagens **sem** data → `dataValidade` deve vir `null` |
| Acurácia de campos | ≥ 90% de `tipoItem` e `dataValidade` corretos | Conjunto golden (US19): comparar JSON extraído × rótulo manual |
| Decisão correta | ≥ 95% de concordância com o supervisor | `status_automatico` × `status` após revisão (consulta SQL no banco) |
| Falhas seguras | 0 itens vencidos aprovados | Teste dos cenários + revisão de todos os NÃO CONFORME do golden |
| Latência | p95 < 15 s por item | `tempo_processamento_ms` e `chamada_ia.latencia_ms` |
| Custo | < R$ 0,05 por item | `tokens_entrada` e `tokens_saida` × preço do modelo |
| Disponibilidade | API responde mesmo com a IA fora | Teste manual com `LLM_BASE_URL` inválida → item vai para revisão |

Consulta útil para acompanhar a concordância IA × supervisor:

```sql
SELECT status_automatico, status, COUNT(*)
FROM item_verificado
WHERE revisado_por IS NOT NULL
GROUP BY status_automatico, status;
```
