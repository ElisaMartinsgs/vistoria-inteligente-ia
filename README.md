# 🦺 Vistoria Inteligente com IA

> Validação automática de vistorias de segurança do trabalho em restaurantes com **OpenCV**, **Tesseract OCR**, **IA (LLM)** e **Java/Spring Boot**.

O técnico de segurança fotografa o item (ex.: o selo do extintor) pelo celular. O sistema trata a imagem, lê o texto, a IA estrutura os dados e as regras em Java devolvem, em poucos segundos:

- ✅ **CONFORME**: segue para o próximo item;
- ⛔ **NÃO CONFORME**: com o motivo em linguagem simples ("O extintor está vencido desde 10/08/2026 (há 101 dias). Providencie a recarga com o fornecedor.");
- 🔎 **EM REVISÃO**: casos duvidosos vão para um supervisor.

**Estudo de caso:** rede de restaurantes *Sabor & Cia* · **Autora:** Elisa Martins · CESAR School, ADS 2026.2 · Residência, etapa Grow Up

---

## Fluxo

```
Foto/PDF ──► OpenCV ──► Tesseract ──► IA (LLM) ──► Regras (Java) ──► PostgreSQL ──► Resultado
          preparar      ler           interpretar   validar          persistir
```

| Etapa | Tecnologia | O que faz |
|---|---|---|
| 1. Imagem | Front-end mobile-first | Técnico tira a foto e escolhe unidade, item e local |
| 2. Tratamento | OpenCV | Corrige perspectiva e rotação, ajusta contraste, remove ruído, converte para preto e branco e **mede a legibilidade** |
| 3. OCR | Tesseract 5 (Tess4J) | Extrai o texto da etiqueta (não entende o significado) |
| 4. Interpretação | LLM (OpenAI, Gemini, Groq, Ollama…) | Devolve um JSON com os campos; o que não encontrar vem `null` |
| 5. Validação | Java / Spring Boot | 10 regras: campos obrigatórios, validade, selo, item presente, item correto, risco × tipo, NR vigente, unidade, confiança da IA, legibilidade |
| 6. Resultado | PostgreSQL + API REST | Salva evidência, parecer, motivos e auditoria da IA |

**A IA extrai; quem decide é o código Java**, que é testado e auditável.

---

## Como rodar

### Opção 1 — Docker (recomendado)

Pré-requisito: Docker Desktop.

```bash
cp .env.example .env        # escolha o provedor de IA e coloque a chave
docker compose up --build
```

Abra **http://localhost:8080** (no celular, use o IP do computador na mesma rede: `http://192.168.x.x:8080`).

Sem chave de API? Use o modo sem IA:

```bash
IA_PROVEDOR=regex docker compose up --build
```

### Opção 2 — Local, sem PostgreSQL (perfil `dev`, banco H2 em memória)

Pré-requisitos: Java 17+, Maven, Tesseract 5 com português.

```bash
# Ubuntu/Debian:  sudo apt install tesseract-ocr tesseract-ocr-por libtesseract-dev
# macOS:          brew install tesseract tesseract-lang
#                 export TESSDATA_PREFIX=$(brew --prefix)/share/tessdata
#                 export JAVA_TOOL_OPTIONS="-Djna.library.path=$(brew --prefix)/lib"
cd backend
export LLM_API_KEY=sk-...          # ou: export IA_PROVEDOR=regex
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Testes

```bash
cd backend && mvn test
```

36 testes de domínio (regras, datas, parser da resposta da IA, extrator por Regex, prompt e os 3 cenários da demonstração com o texto real do OCR). Não precisam de banco, Tesseract nem chave de API.

---

## Demonstração (3 cenários)

As imagens estão em [`exemplos/`](exemplos/) (geradas por `exemplos/gerar_exemplos.py`):

| Imagem | Resultado esperado |
|---|---|
| `1-extintor-conforme.jpg` (validade 08/2027) | ✅ CONFORME |
| `2-extintor-vencido.jpg` (validade 10/08/2026) | ⛔ NÃO CONFORME: vencido, urgência CRÍTICA |
| `3-foto-ilegivel.jpg` (tremida e escura) | ⛔ NÃO CONFORME: "envie outra foto" (sem gastar OCR nem IA) |

Pela API:

```bash
curl -F arquivo=@exemplos/2-extintor-vencido.jpg -F unidadeId=1 -F tecnico="Elisa Martins" \
     -F tipoItem=EXTINTOR -F localizacao=COZINHA http://localhost:8080/api/vistorias/validar
```

> Dica: para a apresentação ter sempre o mesmo resultado (ex.: "vencido há 101 dias"), fixe a data com `DATA_REFERENCIA=2026-11-19` no `.env`.

---

## Configuração da IA

Qualquer API compatível com o formato `/chat/completions` da OpenAI funciona. Veja os exemplos em [`.env.example`](.env.example).

| Variável | Padrão | Descrição |
|---|---|---|
| `IA_PROVEDOR` | `llm` | `llm` ou `regex` (sem IA) |
| `LLM_BASE_URL` | `https://api.openai.com/v1` | Endpoint (OpenAI, Gemini, Groq, Ollama…) |
| `LLM_API_KEY` | vazio | Chave da API |
| `LLM_MODELO` | `gpt-4o-mini` | Modelo |
| `LLM_ENVIAR_IMAGEM` | `false` | `true` = modo multimodal (envia a foto junto) |
| `LLM_TIMEOUT` | `20` | Segundos |
| `DATA_REFERENCIA` | vazio | Data fixa (yyyy-MM-dd) para demonstração |

Se a IA falhar (timeout, limite de uso, fora do ar, JSON inválido, chave errada), o sistema tenta de novo e, se não conseguir, usa o extrator por Regex e manda o item para revisão. **O técnico nunca recebe erro por causa da IA.**

---

## API

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/vistorias/validar` | Valida a foto/PDF de um item (multipart) |
| `POST` | `/api/vistorias` | Abre uma vistoria |
| `GET` | `/api/vistorias?unidadeId=` | Histórico |
| `GET` | `/api/vistorias/{id}` | Detalhe da vistoria |
| `GET` | `/api/itens/{id}` | Detalhe do item (inclui o que cada etapa produziu) |
| `GET` | `/api/itens?status=REVISAO_MANUAL` | Fila do supervisor |
| `PATCH` | `/api/itens/{id}/revisao` | Decisão do supervisor |
| `GET` | `/api/itens/{id}/arquivo` | Evidência original |
| `GET` | `/api/unidades`, `/api/normas`, `/api/tipos-item` | Cadastros |
| `GET` | `/api/painel` | Indicadores de compliance |

Detalhes, exemplos de requisição/resposta e tratamento de erros: [docs/03-banco-e-api.md](docs/03-banco-e-api.md).

---

## Documentação da entrega

| # | Documento |
|---|---|
| 1 | [Problema, solução, personas, casos de borda e entrevistas](docs/01-problema-personas.md) |
| 2 | [Backlog AI-First e critérios de aceite de IA](docs/02-backlog.md) |
| 3 | [Banco de dados, API e tratamento de falhas](docs/03-banco-e-api.md) |
| 4 | [Arquitetura, stack e resiliência](docs/04-arquitetura.md) |
| 5 | [Repositório de prompts e relato do processo](docs/05-prompts-e-relato.md) |

---

## Estrutura

```
backend/src/main/java/br/com/saborecia/vistoria/
├── api/           Controllers REST, DTOs e tratador de erros
├── config/        Configurações (application.yml)
├── dominio/       ★ Java puro: regras, datas, parser da IA, extrator Regex, prompt
├── excecao/       Exceções de negócio e de integração
├── infra/         OpenCV, Tesseract, PDF e cliente do LLM
├── persistencia/  Entidades JPA e repositórios
└── servico/       Orquestração do pipeline e consultas
```

## Tecnologias

Java 17 · Spring Boot 3.3 · OpenCV 4.9 · Tesseract 5 (Tess4J) · PDFBox 3 · PostgreSQL 16 · Flyway · Docker · GitHub Actions · LLM via API compatível com OpenAI
