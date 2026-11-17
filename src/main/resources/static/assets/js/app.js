/* Digital Student Card - page behaviour. No libraries; works without it too (forms still submit). */
(function () {
  'use strict';
  var $ = function (sel, root) { return (root || document).querySelector(sel); };
  var $$ = function (sel, root) { return Array.prototype.slice.call((root || document).querySelectorAll(sel)); };
  var reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  /* ---------- theme ---------- */
  function currentTheme() {
    var set = document.documentElement.getAttribute('data-theme');
    if (set) return set;
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  function paintThemeIcons() {
    var icon = currentTheme() === 'dark' ? '#i-sun' : '#i-moon';
    $$('[data-theme-toggle] use').forEach(function (u) { u.setAttribute('href', icon); });
  }
  $$('[data-theme-toggle]').forEach(function (b) {
    b.addEventListener('click', function () {
      var next = currentTheme() === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      try { localStorage.setItem('dsc-theme', next); } catch (e) { /* ignore */ }
      paintThemeIcons();
    });
  });
  paintThemeIcons();

  /* ---------- navigation ---------- */
  $$('[data-menu]').forEach(function (b) {
    var menu = document.getElementById(b.getAttribute('data-menu'));
    b.addEventListener('click', function () {
      var open = menu.classList.toggle('open');
      b.setAttribute('aria-expanded', open ? 'true' : 'false');
    });
  });
  var app = $('#app');
  $$('[data-sidebar]').forEach(function (b) {
    b.addEventListener('click', function (e) { e.stopPropagation(); app.classList.toggle('nav-open'); });
  });
  document.addEventListener('click', function (e) {
    if (app && app.classList.contains('nav-open') && !e.target.closest('.sidebar')) app.classList.remove('nav-open');
  });
  $$('a[aria-disabled="true"]').forEach(function (a) { a.addEventListener('click', function (e) { e.preventDefault(); }); });

  /* ---------- notices ---------- */
  $$('[data-dismiss]').forEach(function (b) { b.addEventListener('click', function () { b.closest('.notice').remove(); }); });
  $$('[data-autohide]').forEach(function (n) {
    var type = n.className;
    if (type.indexOf('notice--error') >= 0 || type.indexOf('notice--warning') >= 0) return; // keep problems on screen
    setTimeout(function () { if (n.parentNode) n.remove(); }, 6000);
  });
  var summary = $('[data-focus]');
  if (summary) summary.focus();

  /* ---------- passwords ---------- */
  $$('[data-reveal]').forEach(function (b) {
    var input = document.getElementById(b.getAttribute('data-reveal'));
    b.addEventListener('click', function () {
      var show = input.type === 'password';
      input.type = show ? 'text' : 'password';
      b.textContent = show ? 'Hide' : 'Show';
    });
  });
  function rules(v) {
    return { len: v.length >= 8, 'case': /[a-z]/.test(v) && /[A-Z]/.test(v), digit: /\d/.test(v), symbol: /[^A-Za-z0-9]/.test(v) };
  }
  $$('[data-strength]').forEach(function (input) {
    var meter = document.getElementById(input.getAttribute('data-strength'));
    var list = $('[data-rules-for="' + input.id + '"]');
    input.addEventListener('input', function () {
      var r = rules(input.value), score = 0;
      Object.keys(r).forEach(function (k) { if (r[k]) score++; });
      meter.setAttribute('data-score', input.value ? String(score) : '0');
      if (list) $$('[data-rule]', list).forEach(function (li) { li.classList.toggle('ok', r[li.getAttribute('data-rule')]); });
    });
  });
  $$('[data-match]').forEach(function (input) {
    var other = document.getElementById(input.getAttribute('data-match'));
    var msg = input.parentNode.querySelector('[data-match-msg]');
    function check() { if (msg) msg.hidden = !input.value || input.value === other.value; }
    input.addEventListener('input', check);
    other.addEventListener('input', check);
  });

  /* ---------- South African ID number reader ---------- */
  function readId(raw) {
    var id = raw.replace(/\s/g, '');
    if (id.length < 13) return null;
    if (!/^\d{13}$/.test(id)) return { error: 'An ID number has exactly 13 digits.' };
    var yy = +id.substr(0, 2), mm = +id.substr(2, 2), dd = +id.substr(4, 2);
    var now = new Date(), year = 2000 + yy;
    var d = new Date(year, mm - 1, dd);
    if (d > now) { year = 1900 + yy; d = new Date(year, mm - 1, dd); }
    if (d.getMonth() !== mm - 1 || d.getDate() !== dd) return { error: 'The first six digits are not a real date of birth.' };
    if (id[10] !== '0' && id[10] !== '1') return { error: 'Digit 11 must be 0 or 1.' };
    var sum = 0, dbl = false;
    for (var i = 12; i >= 0; i--) { var n = +id[i]; if (dbl) { n *= 2; if (n > 9) n -= 9; } sum += n; dbl = !dbl; }
    if (sum % 10 !== 0) return { error: 'The check digit does not match. Check for a typing mistake.' };
    return {
      birth: d.toLocaleDateString(undefined, { day: 'numeric', month: 'long', year: 'numeric' }),
      gender: +id.substr(6, 4) < 5000 ? 'Female' : 'Male',
      citizen: id[10] === '0' ? 'South African citizen' : 'Permanent resident'
    };
  }
  $$('[data-id-reader]').forEach(function (input) {
    var out = document.getElementById(input.getAttribute('data-id-reader'));
    function update() {
      var r = readId(input.value);
      out.innerHTML = '';
      out.className = 'id-readout';
      if (!r) return;
      out.classList.add('show');
      if (r.error) { out.classList.add('bad'); out.textContent = r.error; return; }
      [['Born', r.birth], ['Gender', r.gender], ['Citizenship', r.citizen]].forEach(function (p) {
        var s = document.createElement('span');
        var b = document.createElement('b'); b.textContent = p[0] + ': ';
        s.appendChild(b); s.appendChild(document.createTextNode(p[1]));
        out.appendChild(s);
      });
    }
    input.addEventListener('input', update);
    if (!input.closest('.field--error')) update(); // the server's message is already shown

  });

  /* ---------- photo preview ---------- */
  $$('[data-preview]').forEach(function (input) {
    var frame = document.getElementById(input.getAttribute('data-preview'));
    input.addEventListener('change', function () {
      var f = input.files && input.files[0];
      var field = input.closest('.field') || input.parentNode;
      var old = field.querySelector('.js-size-error');
      if (old) old.remove();
      if (!f) return;
      if (f.size > 5 * 1024 * 1024 || !/^image\/(jpeg|png)$/.test(f.type)) {
        var e = document.createElement('span');
        e.className = 'error-text js-size-error';
        e.textContent = f.size > 5 * 1024 * 1024 ? 'This photo is larger than 5 MB. Choose a smaller one.' : 'Choose a JPG or PNG photo.';
        input.insertAdjacentElement('afterend', e);
        input.value = '';
        return;
      }
      var img = document.createElement('img');
      img.alt = 'Preview of the chosen photo';
      img.src = URL.createObjectURL(f);
      frame.innerHTML = '';
      frame.appendChild(img);
    });
  });

  /* ---------- card: flip, tilt, live clock, hologram ---------- */
  $$('[data-flip]').forEach(function (flip) {
    flip.addEventListener('click', function () { flip.classList.toggle('is-flipped'); });
  });
  $$('[data-flip-toggle]').forEach(function (b) {
    b.addEventListener('click', function () {
      var flip = $('[data-flip]');
      if (!flip) return;
      flip.classList.toggle('is-flipped');
      var label = b.querySelector('[data-label]'), alt = b.getAttribute('data-alt-label');
      if (label && alt) { b.setAttribute('data-alt-label', label.textContent); label.textContent = alt; }
    });
  });
  if (!reduceMotion && window.matchMedia('(hover: hover)').matches) {
    $$('[data-tilt]').forEach(function (card) {
      card.addEventListener('pointermove', function (e) {
        var r = card.getBoundingClientRect();
        var x = (e.clientX - r.left) / r.width - 0.5, y = (e.clientY - r.top) / r.height - 0.5;
        card.classList.add('is-tilting');
        card.style.setProperty('--tilt-y', (x * 10).toFixed(2) + 'deg');
        card.style.setProperty('--tilt-x', (-y * 10).toFixed(2) + 'deg');
        card.style.setProperty('--sheen-x', (50 + x * 120).toFixed(1) + '%');
      });
      card.addEventListener('pointerleave', function () {
        card.classList.remove('is-tilting');
        card.style.setProperty('--tilt-x', '0deg');
        card.style.setProperty('--tilt-y', '0deg');
      });
    });
  }
  var clocks = $$('[data-clock]'), fullClocks = $$('[data-clock-full]');
  function pad(n) { return (n < 10 ? '0' : '') + n; }
  function tick() {
    var d = new Date();
    var t = pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds());
    clocks.forEach(function (c) { c.textContent = t; });
    fullClocks.forEach(function (c) { c.textContent = d.toLocaleDateString(undefined, { day: 'numeric', month: 'long', year: 'numeric' }) + ', ' + t; });
  }
  if (clocks.length || fullClocks.length) { tick(); setInterval(tick, 1000); }
  // The hologram drifts on live cards (those with a clock), so a still screenshot looks different from the real card.
  if (!reduceMotion) {
    var live = $$('.idcard').filter(function (c) { return c.querySelector('[data-clock]'); });
    if (live.length) {
      var t0 = performance.now();
      (function drift(now) {
        var x = 50 + Math.sin((now - t0) / 1600) * 45;
        live.forEach(function (c) { if (!c.classList.contains('is-tilting')) c.style.setProperty('--sheen-x', x.toFixed(1) + '%'); });
        requestAnimationFrame(drift);
      })(t0);
    }
  }

  /* keep the screen on while a card is shown full screen */
  if ($('[data-wake-lock]') && navigator.wakeLock) {
    var lock = null;
    var request = function () { navigator.wakeLock.request('screen').then(function (l) { lock = l; }).catch(function () {}); };
    request();
    document.addEventListener('visibilitychange', function () { if (document.visibilityState === 'visible' && !lock) request(); });
  }

  /* ---------- dialogs and confirmations ---------- */
  $$('[data-open-dialog]').forEach(function (b) {
    b.addEventListener('click', function () {
      var d = document.getElementById(b.getAttribute('data-open-dialog'));
      if (d && d.showModal) d.showModal();
    });
  });
  $$('[data-close-dialog]').forEach(function (b) {
    b.addEventListener('click', function () { b.closest('dialog').close(); });
  });
  $$('[data-must-equal]').forEach(function (input) {
    var target = input.form.querySelector('[data-confirm-target]');
    input.addEventListener('input', function () { target.disabled = input.value.replace(/\s/g, '') !== input.getAttribute('data-must-equal'); });
  });
  var confirmDialog = null;
  function askConfirm(message, onYes) {
    if (!window.HTMLDialogElement) { if (window.confirm(message)) onYes(); return; }
    if (!confirmDialog) {
      confirmDialog = document.createElement('dialog');
      confirmDialog.innerHTML = '<div class="panel__body"><h2>Are you sure?</h2><p class="muted" data-msg></p></div>' +
        '<div class="dialog__actions"><button type="button" class="btn btn--quiet" data-no>Cancel</button><button type="button" class="btn btn--primary" data-yes>Continue</button></div>';
      document.body.appendChild(confirmDialog);
      confirmDialog.querySelector('[data-no]').addEventListener('click', function () { confirmDialog.close(); });
    }
    confirmDialog.querySelector('[data-msg]').textContent = message;
    var yes = confirmDialog.querySelector('[data-yes]');
    var fresh = yes.cloneNode(true);
    yes.parentNode.replaceChild(fresh, yes);
    fresh.addEventListener('click', function () { confirmDialog.close(); onYes(); });
    confirmDialog.showModal();
  }
  $$('form[data-confirm]').forEach(function (form) {
    form.addEventListener('submit', function (e) {
      if (form.dataset.confirmed) return;
      e.preventDefault();
      askConfirm(form.getAttribute('data-confirm'), function () { form.dataset.confirmed = '1'; form.requestSubmit ? form.requestSubmit() : form.submit(); });
    });
  });

  /* ---------- tables: select rows for bulk actions ---------- */
  $$('form[data-bulk]').forEach(function (form) {
    var boxes = $$('input[type=checkbox][name=sn]', form);
    var all = $('[data-select-all]', form);
    var bar = $('[data-bulkbar]', form);
    var count = $('[data-selected-count]', form);
    var needs = $$('[data-needs-selection]', form);
    function refresh() {
      var n = boxes.filter(function (b) { return b.checked; }).length;
      if (bar) bar.classList.toggle('show', n > 0);
      if (count) count.textContent = n;
      needs.forEach(function (b) { b.disabled = n === 0; });
      if (all) { all.checked = n > 0 && n === boxes.length; all.indeterminate = n > 0 && n < boxes.length; }
    }
    boxes.forEach(function (b) { b.addEventListener('change', refresh); });
    if (all) all.addEventListener('change', function () { boxes.forEach(function (b) { b.checked = all.checked; }); refresh(); });
    refresh();
  });
  $$('form[data-autosubmit] select').forEach(function (s) {
    s.addEventListener('change', function () { s.form.submit(); });
  });

  /* ---------- stop double submissions ---------- */
  $$('form[method="post"]').forEach(function (form) {
    form.addEventListener('submit', function (e) {
      if (e.defaultPrevented) return;
      setTimeout(function () {
        $$('button[type=submit]', form).forEach(function (b) { b.disabled = true; });
      }, 0);
    });
  });
})();
