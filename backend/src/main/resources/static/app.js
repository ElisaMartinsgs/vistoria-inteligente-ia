/* Vistoria Inteligente — front-end mobile-first (JavaScript puro, sem build). */
(() => {
  'use strict';

  const $ = (sel, raiz = document) => raiz.querySelector(sel);
  const STATUS = {
    CONFORME: { texto: 'CONFORME', icone: '✅' },
    NAO_CONFORME: { texto: 'NÃO CONFORME', icone: '⛔' },
    REVISAO_MANUAL: { texto: 'EM REVISÃO', icone: '🔎' },
  };
  const ORIGEM = {
    LLM: 'IA (LLM)', REGEX: 'Regex (modo sem IA)', REGEX_FALLBACK: 'Regex (IA indisponível)', NENHUMA: 'não extraído (foto ilegível)',
  };
  let tipos = [];
  let vistoriaAtual = null; // { id, unidade, itens }

  // ---------------- utilidades ----------------
  const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const dataBR = (iso) => (iso ? iso.slice(0, 10).split('-').reverse().join('/') : '—');
  const nomeTipo = (codigo) => tipos.find((t) => t.codigo === codigo)?.nome ?? (codigo ? codigo.toLowerCase() : '—');

  const guardar = (k, v) => { try { localStorage.setItem(k, v); } catch (_) { /* armazenamento indisponível */ } };
  const ler = (k) => { try { return localStorage.getItem(k) || ''; } catch (_) { return ''; } };

  async function api(url, opcoes = {}) {
    let resp;
    try {
      resp = await fetch(url, opcoes);
    } catch (_) {
      throw new Error('Sem conexão com o servidor. Verifique a internet e tente de novo.');
    }
    const tipo = resp.headers.get('content-type') || '';
    const corpo = tipo.includes('json') ? await resp.json() : null;
    if (!resp.ok) throw new Error(corpo?.mensagem || corpo?.detail || `Erro ${resp.status}`);
    return corpo;
  }

  // ---------------- abas ----------------
  document.querySelectorAll('.aba').forEach((b) => b.addEventListener('click', () => abrirAba(b.dataset.aba)));
  function abrirAba(nome) {
    document.querySelectorAll('.aba').forEach((b) => b.classList.toggle('ativa', b.dataset.aba === nome));
    document.querySelectorAll('.painel-aba').forEach((s) => s.classList.toggle('ativa', s.id === `aba-${nome}`));
    if (nome === 'historico') carregarHistorico();
    if (nome === 'revisao') carregarRevisao();
    if (nome === 'painel') carregarPainel();
  }

  // ---------------- carga inicial ----------------
  async function iniciar() {
    $('#tecnico').value = ler('tecnico');
    $('#supervisor').value = ler('supervisor');
    try {
      const [unidades, listaTipos] = await Promise.all([api('/api/unidades'), api('/api/tipos-item')]);
      tipos = listaTipos;
      const opcoes = unidades.map((u) => `<option value="${u.id}">${esc(u.nome)}</option>`).join('');
      $('#unidade').innerHTML = opcoes;
      $('#filtroUnidade').insertAdjacentHTML('beforeend', opcoes);
      $('#unidade').value = ler('unidade') || unidades[0]?.id || '';
      $('#tipoItem').innerHTML = tipos.map((t) => `<option value="${t.codigo}">${esc(t.nome)}</option>`).join('');
    } catch (e) {
      mostrarErro(e.message);
    }
    atualizarContadorRevisao();
  }

  // ---------------- nova vistoria ----------------
  $('#arquivo').addEventListener('change', () => {
    const f = $('#arquivo').files[0];
    const img = $('#preview');
    if (!f) return;
    if (f.type.startsWith('image/')) {
      img.src = URL.createObjectURL(f);
      img.hidden = false;
      $('#fotoTexto').hidden = true;
    } else {
      img.hidden = true;
      $('#fotoTexto').hidden = false;
      $('#fotoTexto').innerHTML = `📄 ${esc(f.name)}<br><small>toque para trocar</small>`;
    }
  });

  $('#formItem').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    esconderErro();
    const f = $('#arquivo').files[0];
    const tecnico = $('#tecnico').value.trim();
    if (!tecnico) return mostrarErro('Informe o nome do técnico.');
    if (!f) return mostrarErro('Tire uma foto ou escolha um arquivo do item.');
    if (f.size > 10 * 1024 * 1024) return mostrarErro('O arquivo passa de 10 MB. Tire uma foto com menor resolução.');

    guardar('tecnico', tecnico);
    guardar('unidade', $('#unidade').value);

    const dados = new FormData();
    dados.append('arquivo', f);
    dados.append('unidadeId', $('#unidade').value);
    dados.append('tecnico', tecnico);
    dados.append('tipoItem', $('#tipoItem').value);
    dados.append('localizacao', $('#localizacao').value);
    if (vistoriaAtual) dados.append('vistoriaId', vistoriaAtual.id);

    $('#enviar').disabled = true;
    $('#formItem').hidden = true;
    $('#resultado').hidden = true;
    const animacao = animarEtapas();
    try {
      const r = await api('/api/vistorias/validar', { method: 'POST', body: dados });
      animacao.parar(true);
      if (!vistoriaAtual) vistoriaAtual = { id: r.vistoriaId, unidade: r.unidade, itens: 0 };
      vistoriaAtual.itens += 1;
      mostrarFaixaVistoria();
      mostrarResultado(r);
      atualizarContadorRevisao();
    } catch (e) {
      animacao.parar(false);
      $('#formItem').hidden = false;
      mostrarErro(e.message);
    } finally {
      $('#enviar').disabled = false;
    }
  });

  function animarEtapas() {
    const caixa = $('#processando');
    const itens = [...caixa.querySelectorAll('li')];
    caixa.hidden = false;
    let i = 0;
    const marcar = () => itens.forEach((li, k) => {
      li.classList.toggle('feita', k < i);
      li.classList.toggle('atual', k === i);
    });
    marcar();
    const t = setInterval(() => { if (i < itens.length - 1) { i += 1; marcar(); } }, 1200);
    return {
      parar(ok) {
        clearInterval(t);
        if (ok) { i = itens.length; marcar(); }
        setTimeout(() => { caixa.hidden = true; }, ok ? 300 : 0);
      },
    };
  }

  function mostrarFaixaVistoria() {
    const f = $('#vistoriaAtual');
    f.hidden = !vistoriaAtual;
    if (vistoriaAtual) {
      f.innerHTML = `Vistoria <strong>#${vistoriaAtual.id}</strong> · ${esc(vistoriaAtual.unidade)} · ${vistoriaAtual.itens} item(ns)`;
    }
    $('#unidade').disabled = !!vistoriaAtual;
  }

  function mostrarResultado(r) {
    const el = $('#tplResultado').content.firstElementChild.cloneNode(true);
    el.classList.add(`st-${r.status}`);
    $('.icone', el).textContent = STATUS[r.status].icone;
    $('.status', el).textContent = STATUS[r.status].texto;
    $('.resumo', el).textContent = r.resumo;

    // Motivos que já estão no resumo não são repetidos; aparecem só os adicionais (alertas, pontos de revisão).
    $('.motivos', el).innerHTML = r.motivos
      .filter((m) => !r.resumo.includes(m.mensagem))
      .map((m) => `<li class="${m.severidade}">${esc(m.mensagem)}<small>${esc(m.regra)}</small></li>`).join('');

    const dados = [
      ['Item', nomeTipo(r.tipoItem || r.tipoEsperado)],
      ['Local', r.localizacao],
      ['Validade', dataBR(r.dataValidade)],
      ['Risco', r.classificacaoRisco],
      ['Norma', r.norma],
      ['Urgência', r.urgencia],
    ];
    $('.dados', el).innerHTML = dados.map(([k, v]) => `<dt>${k}</dt><dd>${esc(v ?? '—')}</dd>`).join('');

    const p = r.pipeline;
    const num = (n, d = 1) => (n == null ? '—' : Number(n).toFixed(d));
    $('.pipeline', el).innerHTML = `
      <h4>1 · OpenCV</h4>
      <p>Nitidez ${num(p.nitidez)} · Brilho ${num(p.brilho)} · Contraste ${num(p.contraste)}</p>
      <h4>2 · Tesseract (confiança ${num(p.confiancaOcr)}%)</h4>
      <pre>${esc(p.textoOcr || '(sem texto)')}</pre>
      <h4>3 · Extração: ${esc(ORIGEM[p.origemExtracao] || p.origemExtracao)}</h4>
      <pre>${esc(JSON.stringify(p.dadosExtraidos, null, 2))}</pre>
      <h4>4 · Regras aplicadas</h4>
      <p>${esc((p.regrasAplicadas || []).join(', '))}</p>
      <p><small>Tempo total: ${p.tempoProcessamentoMs} ms</small></p>`;

    el.querySelector('[data-acao="outro"]').addEventListener('click', novoItem);
    el.querySelector('[data-acao="finalizar"]').addEventListener('click', () => {
      vistoriaAtual = null;
      mostrarFaixaVistoria();
      novoItem();
    });

    const caixa = $('#resultado');
    caixa.replaceChildren(el);
    caixa.hidden = false;
    caixa.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  function novoItem() {
    $('#resultado').hidden = true;
    $('#formItem').hidden = false;
    $('#formItem').reset();
    $('#tecnico').value = ler('tecnico');
    if (vistoriaAtual) $('#unidade').disabled = true; else $('#unidade').value = ler('unidade');
    $('#preview').hidden = true;
    $('#fotoTexto').hidden = false;
    $('#fotoTexto').innerHTML = '📷 Tirar foto do item<br><small>ou escolher JPG, PNG ou PDF (até 10 MB)</small>';
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function mostrarErro(msg) { const e = $('#erroForm'); e.textContent = msg; e.hidden = false; }
  function esconderErro() { $('#erroForm').hidden = true; }

  // ---------------- histórico ----------------
  $('#filtroUnidade').addEventListener('change', carregarHistorico);
  async function carregarHistorico() {
    const alvo = $('#listaVistorias');
    alvo.innerHTML = '<p class="vazio">Carregando…</p>';
    try {
      const id = $('#filtroUnidade').value;
      const lista = await api(`/api/vistorias${id ? `?unidadeId=${id}` : ''}`);
      if (!lista.length) { alvo.innerHTML = '<p class="vazio">Nenhuma vistoria ainda.</p>'; return; }
      alvo.innerHTML = lista.map((v) => `
        <article class="cartao vistoria">
          <h3>#${v.id} · ${esc(v.unidade)} ${v.statusGeral ? `<span class="etiqueta ${v.statusGeral}">${STATUS[v.statusGeral].texto}</span>` : ''}</h3>
          <p class="meta">${dataBR(v.dataVistoria)} · ${esc(v.tecnico)} · ${v.conformes}/${v.totalItens} conformes</p>
          <ul>${v.itens.map((i) => `<li><span class="etiqueta ${i.status}">${STATUS[i.status].texto}</span>
            <strong>${esc(nomeTipo(i.tipoItem || i.tipoEsperado))}</strong> — ${esc(i.resumo)}</li>`).join('')}</ul>
        </article>`).join('');
    } catch (e) {
      alvo.innerHTML = `<p class="erro">${esc(e.message)}</p>`;
    }
  }

  // ---------------- revisão (supervisor) ----------------
  async function atualizarContadorRevisao() {
    try {
      const lista = await api('/api/itens?status=REVISAO_MANUAL');
      const c = $('#contadorRevisao');
      c.textContent = lista.length;
      c.hidden = lista.length === 0;
    } catch (_) { /* silencioso */ }
  }

  async function carregarRevisao() {
    const alvo = $('#listaRevisao');
    alvo.innerHTML = '<p class="vazio">Carregando…</p>';
    try {
      const lista = await api('/api/itens?status=REVISAO_MANUAL');
      if (!lista.length) { alvo.innerHTML = '<p class="vazio">Nenhum item aguardando revisão. 🎉</p>'; return; }
      alvo.innerHTML = lista.map((i) => `
        <article class="cartao revisao" data-id="${i.itemId}">
          <h3>${esc(nomeTipo(i.tipoItem || i.tipoEsperado))} · ${esc(i.unidade)}</h3>
          <img src="/api/itens/${i.itemId}/arquivo" alt="Evidência enviada" loading="lazy" onerror="this.hidden=true">
          <ul class="motivos">${i.motivos.map((m) => `<li class="${m.severidade}">${esc(m.mensagem)}<small>${esc(m.detalheTecnico)}</small></li>`).join('')}</ul>
          <p class="meta">Validade lida: <strong>${dataBR(i.dataValidade)}</strong></p>
          <label>Observação<textarea rows="2" maxlength="1000"></textarea></label>
          <div class="botoes">
            <button class="botao ok" data-decisao="CONFORME">Aprovar</button>
            <button class="botao ruim" data-decisao="NAO_CONFORME">Reprovar</button>
          </div>
        </article>`).join('');
      alvo.querySelectorAll('[data-decisao]').forEach((b) => b.addEventListener('click', decidir));
    } catch (e) {
      alvo.innerHTML = `<p class="erro">${esc(e.message)}</p>`;
    }
  }

  async function decidir(ev) {
    const cartao = ev.target.closest('[data-id]');
    const supervisor = $('#supervisor').value.trim();
    if (!supervisor) { alert('Informe o nome do supervisor no topo da tela.'); $('#supervisor').focus(); return; }
    guardar('supervisor', supervisor);
    ev.target.disabled = true;
    try {
      await api(`/api/itens/${cartao.dataset.id}/revisao`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: ev.target.dataset.decisao, supervisor, observacao: $('textarea', cartao).value }),
      });
      cartao.remove();
      atualizarContadorRevisao();
      if (!$('#listaRevisao [data-id]')) carregarRevisao();
    } catch (e) {
      ev.target.disabled = false;
      alert(e.message);
    }
  }

  // ---------------- painel (compliance) ----------------
  async function carregarPainel() {
    const alvo = $('#painel');
    alvo.innerHTML = '<p class="vazio">Carregando…</p>';
    try {
      const p = await api('/api/painel');
      alvo.innerHTML = `
        <div class="kpis">
          <div class="kpi"><span>Itens vistoriados</span><strong>${p.totalItens}</strong></div>
          <div class="kpi"><span>Taxa de conformidade</span><strong>${p.taxaConformidade}%</strong>
            <div class="barra"><i style="width:${p.taxaConformidade}%"></i></div></div>
          <div class="kpi"><span>Não conformes</span><strong>${p.naoConformes}</strong></div>
          <div class="kpi"><span>Aguardando revisão</span><strong>${p.emRevisao}</strong></div>
        </div>
        <div class="cartao"><h3>Principais motivos de reprovação</h3>
          ${p.principaisMotivos.length ? `<table class="tabela"><tr><th>Regra</th><th>Ocorrências</th></tr>
            ${p.principaisMotivos.map((m) => `<tr><td>${esc(m.regra)}</td><td>${m.quantidade}</td></tr>`).join('')}</table>`
            : '<p class="vazio">Sem reprovações.</p>'}
        </div>
        <div class="cartao"><h3>Por unidade</h3>
          ${p.porUnidade.length ? `<table class="tabela"><tr><th>Unidade</th><th>Itens</th><th>Conformidade</th></tr>
            ${p.porUnidade.map((u) => `<tr><td>${esc(u.unidade)}</td><td>${u.total}</td><td>${u.taxaConformidade}%</td></tr>`).join('')}</table>`
            : '<p class="vazio">Sem dados.</p>'}
        </div>`;
    } catch (e) {
      alvo.innerHTML = `<p class="erro">${esc(e.message)}</p>`;
    }
  }

  iniciar();
})();
