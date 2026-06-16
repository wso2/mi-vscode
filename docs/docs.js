/* docs.js — shared behaviour for MI XML LS documentation */
(function () {
  'use strict';

  /* ── Sidebar toggle ──────────────────────────────────────────────────── */

  var KEY     = 'mi-docs-sidebar';
  var body    = document.body;
  var toggler = document.getElementById('sidebarToggle');
  var overlay = document.getElementById('sidebarOverlay');

  function isMobile() { return window.innerWidth <= 900; }

  if (toggler && !isMobile() && localStorage.getItem(KEY) === 'hidden')
    body.classList.add('sidebar-hidden');

  function toggleSidebar() {
    if (isMobile()) {
      body.classList.toggle('sidebar-open');
    } else {
      var hidden = body.classList.toggle('sidebar-hidden');
      localStorage.setItem(KEY, hidden ? 'hidden' : 'visible');
    }
  }

  if (toggler) toggler.addEventListener('click', toggleSidebar);
  if (overlay)  overlay.addEventListener('click', function () { body.classList.remove('sidebar-open'); });
  window.addEventListener('resize', function () { if (!isMobile()) body.classList.remove('sidebar-open'); });

  /* ── Diagram zoom ────────────────────────────────────────────────────── */

  var STEP = 0.2;
  var MIN  = 0.25;
  var MAX  = 3;

  function setupZoom(fig, svg) {
    if (fig.dataset.dzReady) return;
    fig.dataset.dzReady = '1';

    var level  = 1;
    var origW  = svg.getBoundingClientRect().width  || parseFloat(svg.getAttribute('width'))  || 0;
    var origH  = svg.getBoundingClientRect().height || parseFloat(svg.getAttribute('height')) || 0;

    /* toolbar */
    var bar = document.createElement('div');
    bar.className = 'dz-bar';
    bar.innerHTML =
      '<button class="dz-btn" data-d="-1" title="Zoom out (−)">&#8722;</button>' +
      '<span  class="dz-pct">100%</span>' +
      '<button class="dz-btn" data-d="1"  title="Zoom in (+)">&#43;</button>' +
      '<button class="dz-btn" data-d="0"  title="Reset zoom">↺</button>';
    fig.insertBefore(bar, fig.firstChild);

    /* Ctrl+scroll hint */
    var hint = document.createElement('div');
    hint.className = 'dz-hint';
    hint.textContent = 'Ctrl + scroll to zoom';
    fig.appendChild(hint);

    function applyZoom() {
      if (origW) svg.setAttribute('width',  origW * level);
      if (origH) svg.setAttribute('height', origH * level);
      bar.querySelector('.dz-pct').textContent = Math.round(level * 100) + '%';
    }

    bar.addEventListener('click', function (e) {
      var btn = e.target.closest('[data-d]');
      if (!btn) return;
      var d = +btn.dataset.d;
      level = d === 0 ? 1 : Math.max(MIN, Math.min(MAX, level + d * STEP));
      applyZoom();
    });

    fig.addEventListener('wheel', function (e) {
      if (!e.ctrlKey && !e.metaKey) return;
      e.preventDefault();
      level = Math.max(MIN, Math.min(MAX, level + (e.deltaY < 0 ? STEP : -STEP)));
      applyZoom();
    }, { passive: false });
  }

  function watchForSVGs() {
    var figs = Array.from(document.querySelectorAll('figure.mermaid'));
    if (!figs.length) return;

    /* handle figures whose SVGs are already in the DOM */
    figs.forEach(function (fig) {
      var svg = fig.querySelector('svg');
      if (svg) setupZoom(fig, svg);
    });

    /* observe the rest as mermaid renders them */
    var remaining = figs.filter(function (f) { return !f.dataset.dzReady; });
    if (!remaining.length) return;

    var observer = new MutationObserver(function (mutations) {
      mutations.forEach(function (m) {
        m.addedNodes.forEach(function (node) {
          if (node.nodeName !== 'svg') return;
          var fig = node.closest('figure.mermaid');
          if (fig) setupZoom(fig, node);
        });
      });
      if (!document.querySelector('figure.mermaid:not([data-dz-ready])'))
        observer.disconnect();
    });

    observer.observe(document.body, { childList: true, subtree: true });
    /* safety disconnect after 15 s */
    setTimeout(function () { observer.disconnect(); }, 15000);
  }

  if (document.readyState === 'loading')
    document.addEventListener('DOMContentLoaded', watchForSVGs);
  else
    watchForSVGs();


})();
