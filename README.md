# 🦺 Vistoria Inteligente com IA — Gestão de Segurança do Trabalho em Restaurantes

> Automação da validação de relatórios de vistoria e conformidade com normas regulamentadoras (NR) através de Visão Computacional, OCR e Modelos de Linguagem (LLM).

---

## 📌 Sobre o Projeto

Em estabelecimentos como restaurantes e cozinhas industriais, o cumprimento rigoroso de normas de segurança do trabalho (como NR-12, NR-23 e NR-26) é obrigatório por lei. Contudo, a realização de vistorias ainda depende frequentemente de checklists em papel ou folhas de cálculo, atrasando a geração de planos de ação e a recarga atempada de equipamentos críticos (como extintores de incêndio).

O **Vistoria Inteligente** automatiza a conferência de evidências fotográficas e itens de conformidade em campo, permitindo que técnicos de segurança e gestores de unidades validem dados em tempo real, eliminando o vaivém da conferência manual e o risco de autuações.

---

## 👥 Público-Alvo e Casos de Uso

* **Técnico(a) de Segurança do Trabalho:** Registo rápido e simplificado de evidências diretamente do telemóvel durante a inspeção.
* **Gestor(a) do Restaurante:** Acesso imediato a relatórios validados, com destaque de inconformidades e ações corretivas necessárias.
* **Franquias e Redes de Restauração:** Painel centralizado com o panorama de conformidade entre diversas filiais.
* **Equipas de Compliance / RH:** Monitorização de indicadores de risco, volume de vistorias e principais causas de reprovação.

---

## 🔄 Fluxo de Processamento (Pipeline)

A solução organiza o fluxo de dados em etapas bem definidas: preparar, ler, interpretar, validar e persistir.

[ Fotografia do Item ]
│
▼
[ 1. Pré-processamento — OpenCV ]
(Correção de rotação, perspetiva, contraste e redução de ruído)
│
▼
[ 2. Extração de Texto — Tesseract OCR ]
(Leitura de etiquetas, rótulos e selos de inspeção)
│
▼
[ 3. Interpretação Semântica — LLM / IA ]
(Estruturação dos dados extraídos para o schema JSON / Java Record)
│
▼
[ 4. Regras de Negócio e Validação — Spring Boot ]
(Verificação de vigência das NRs, formato de datas e prazos de validade)
│
▼
[ 5. Persistência e Resposta — PostgreSQL & API REST ]
(Devolução do parecer: CONFORME / NÃO CONFORME com justificações claras)

---

## ⚙️ Regras de Validação e Critérios

* **Aprovação (`CONFORME`):** O item está visível e legível, presente na unidade correta, dentro do prazo de validade legal, classificado no grau de risco adequado e de acordo com a respetiva norma técnica vigente.
* **Reprovação (`NÃO CONFORME`):** 
  * Campos essenciais ausentes ou não identificáveis (`null`).
  * Equipamento vencido ou fora do prazo de inspeção regulamentar.
  * Fotografias ilegíveis, escuras, com desfoque excessivo ou objeto incompatível.
  * Falhas no enquadramento normativo.
* **Casos Duvidosos:** Encaminhados automaticamente para revisão humana por um supervisor.

---

## 🛠️ Tecnologias Utilizadas

* **Linguagem & Backend:** Java 17+, Spring Boot
* **Visão Computacional & OCR:** OpenCV, Tesseract OCR
* **Inteligência Artificial:** Modelos de Linguagem (LLM) integrados via API
* **Base de Dados:** PostgreSQL
* **Interface / Mobile:** Web Mobile-First

---

## 🎯 Escopo do MVP (Mínimo Produto Viável)

* [x] Interface mobile-first para upload da fotografia e visualização do parecer final.
* [x] Endpoint REST em Spring Boot (`POST /vistorias/validar`)[cite: 15].
* [x] Pipeline mínimo automatizado: OpenCV $\rightarrow$ Tesseract $\rightarrow$ LLM $\rightarrow$ Regras de validação em Java.
* [x] Persistência do histórico das vistorias em PostgreSQL.
* [x] Demonstração com 3 cenários práticos: item conforme, item vencido e imagem ilegível.

---

## 👩‍💻 Autoria e Contexto Académico

* **Autora:** Elisa Martins
* **Instituição:** CESAR School — Análise e Desenvolvimento de Sistemas (ADS Regular 2026.2)
* **Programa:** Residência de Software, Etapa Grow Up[cite: 14]
* **Estudo de Caso:** Rede de Restaurantes *Sabor & Cia*[cite: 14]
