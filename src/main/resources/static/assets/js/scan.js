/* Gate scanner: reads QR codes from the camera and checks each one with the server. */
(function () {
  'use strict';
  var root = document.querySelector('[data-scanner]');
  if (!root) return;
  var $ = function (sel) { return root.querySelector(sel); };
  var checkUrl = root.getAttribute('data-check-url');
  var meta = function (n) { var m = document.querySelector('meta[name="' + n + '"]'); return m ? m.getAttribute('content') : ''; };
  var csrf = meta('_csrf'), csrfHeader = meta('_csrf_header') || 'X-CSRF-TOKEN';
  var viewfinder = document.getElementById('viewfinder');
  var select = $('[data-camera-select]');
  var startBtn = $('[data-start-camera]');
  var stopBtn = $('[data-stop-camera]');
  var locationInput = $('[data-location]');
  var soundBox = $('[data-sound]');
  var resultSlot = $('[data-result]');
  var emptyPanel = $('[data-result-empty]');
  var history = $('[data-history]');
  var tally = $('[data-tally]');
  var template = document.getElementById('result-template');
  var scanner = null, running = false, busy = false, lastCode = '', lastAt = 0, count = 0, valid = 0;

  try { locationInput.value = localStorage.getItem('dsc-scan-location') || ''; } catch (e) { /* ignore */ }
  locationInput.addEventListener('change', function () {
    try { localStorage.setItem('dsc-scan-location', locationInput.value); } catch (e) { /* ignore */ }
  });

  /* ---------- camera ---------- */
  function pickBackCamera(cams) {
    for (var i = 0; i < cams.length; i++) {
      if (/back|rear|environment/i.test(cams[i].label)) return cams[i].id;
    }
    return cams[cams.length - 1].id;
  }

  function start(cameraId) {
    if (typeof Html5Qrcode === 'undefined') {
      showMessage('fail', 'The scanner could not load', 'Reload the page. You can still type student numbers below.');
      return;
    }
    if (!scanner) scanner = new Html5Qrcode('reader', { verbose: false });
    var config = {
      fps: 10,
      qrbox: function (w, h) { var s = Math.floor(Math.min(w, h) * 0.68); return { width: s, height: s }; },
      aspectRatio: 1
    };
    scanner.start(cameraId, config, onCode, function () { /* no code in this frame */ })
      .then(function () {
        running = true;
        viewfinder.classList.remove('is-idle');
        $('[data-idle]').hidden = true;
        stopBtn.hidden = false;
      })
      .catch(function (err) {
        showMessage('fail', 'The camera did not start', cameraHelp(err));
      });
  }

  function cameraHelp(err) {
    var m = String(err || '');
    if (/NotAllowed|Permission/i.test(m)) return 'Camera permission was refused. Allow the camera in the browser address bar, then press Start camera again.';
    if (!window.isSecureContext) return 'Browsers only allow the camera on https:// addresses or on localhost. Open this page that way, or type numbers below.';
    if (/NotFound|no camera/i.test(m)) return 'No camera was found on this device. Type student numbers below instead.';
    return 'The camera could not be opened (' + m + '). Close other apps that use the camera and try again.';
  }

  startBtn.addEventListener('click', function () {
    if (typeof Html5Qrcode === 'undefined') { start(); return; }
    Html5Qrcode.getCameras().then(function (cams) {
      if (!cams || !cams.length) { showMessage('fail', 'No camera found', 'Type student numbers below instead.'); return; }
      select.innerHTML = '';
      cams.forEach(function (c, i) {
        var o = document.createElement('option');
        o.value = c.id; o.textContent = c.label || ('Camera ' + (i + 1));
        select.appendChild(o);
      });
      select.disabled = false;
      var id = pickBackCamera(cams);
      select.value = id;
      start(id);
    }).catch(function (err) { showMessage('fail', 'The camera did not start', cameraHelp(err)); });
  });

  select.addEventListener('change', function () {
    if (!scanner) return;
    var go = function () { start(select.value); };
    if (running) scanner.stop().then(go).catch(go); else go();
  });

  stopBtn.addEventListener('click', function () {
    if (!scanner || !running) return;
    scanner.stop().then(function () {
      running = false;
      viewfinder.classList.add('is-idle');
      $('[data-idle]').hidden = false;
      stopBtn.hidden = true;
    });
  });

  function onCode(text) {
    var now = Date.now();
    if (busy || (text === lastCode && now - lastAt < 4000)) return; // same card still in view
    lastCode = text; lastAt = now;
    check(text, false);
  }

  /* ---------- manual entry ---------- */
  root.querySelector('[data-manual]').addEventListener('submit', function (e) {
    e.preventDefault();
    var input = document.getElementById('manual-number');
    var v = input.value.replace(/\s/g, '');
    if (!/^\d{6,12}$/.test(v)) { input.focus(); input.setCustomValidity('Enter the 9-digit student number.'); input.reportValidity(); return; }
    input.setCustomValidity('');
    check(v, true);
    input.value = '';
  });
  document.getElementById('manual-number').addEventListener('input', function () { this.setCustomValidity(''); });

  /* ---------- server check ---------- */
  function check(code, typed) {
    busy = true;
    var body = new URLSearchParams();
    body.set('code', code);
    body.set('location', locationInput.value || '');
    if (typed) body.set('typed', 'true');
    fetch(checkUrl, {
      method: 'POST',
      credentials: 'same-origin',
      headers: (function () { var h = { 'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest' }; h[csrfHeader] = csrf; return h; })(),
      body: body.toString()
    }).then(function (r) {
      if (r.status === 401) throw new Error('signed-out');
      return r.json().then(function (j) { if (!r.ok) throw new Error(j.error || 'failed'); return j; });
    }).then(function (data) {
      render(data);
      feedback(data.tone);
      remember(data);
    }).catch(function (err) {
      if (err.message === 'signed-out') {
        showMessage('fail', 'You were signed out', 'Your session ended. Reload the page and sign in again.');
      } else {
        showMessage('fail', 'Could not check the card', err.message === 'failed' ? 'The server did not answer. Check the connection and try again.' : err.message);
      }
    }).then(function () { setTimeout(function () { busy = false; }, 600); });
  }

  function render(data) {
    var node = template.content.cloneNode(true);
    var section = node.querySelector('.verdict');
    section.classList.add('verdict--' + data.tone);
    node.querySelector('.verdict__band use').setAttribute('href', data.tone === 'pass' ? '#i-check' : (data.tone === 'warn' ? '#i-warning' : '#i-x'));
    set(node, 'headline', data.headline);
    set(node, 'advice', data.advice);
    set(node, 'checkedAt', data.checkedAt);
    var st = data.student;
    if (st) {
      node.querySelector('[data-f="photo"]').src = st.photo;
      set(node, 'name', st.name);
      set(node, 'number', st.number);
      set(node, 'campus', st.campus || 'Not recorded');
      set(node, 'qualification', st.qualification || 'Not recorded');
      set(node, 'expiry', st.expiry);
      if (st.reason) { set(node, 'reason', st.reason); } else {
        node.querySelector('[data-reason-label]').remove();
        node.querySelector('[data-f="reason"]').remove();
      }
      if (st.profile) node.querySelector('[data-f="profile"]').href = st.profile;
      else node.querySelector('[data-profile-wrap]').remove();
    } else {
      node.querySelector('[data-student]').remove();
    }
    resultSlot.innerHTML = '';
    resultSlot.appendChild(node);
    emptyPanel.hidden = true;
  }

  function set(node, field, value) {
    var el = node.querySelector('[data-f="' + field + '"]');
    if (el) el.textContent = value == null ? '' : value;
  }

  function showMessage(tone, headline, advice) {
    render({ tone: tone, headline: headline, advice: advice, checkedAt: new Date().toLocaleTimeString() });
  }

  function remember(data) {
    count++;
    if (data.result === 'VALID') valid++;
    tally.textContent = count + ' checked, ' + valid + ' valid';
    if (history.firstElementChild && history.firstElementChild.classList.contains('muted')) history.innerHTML = '';
    var li = document.createElement('li');
    var badge = document.createElement('span');
    badge.className = 'status status--' + (data.result === 'VALID' ? 'valid' : 'failed');
    badge.textContent = data.result === 'VALID' ? 'Valid' : data.headline;
    var who = document.createElement('span');
    who.textContent = data.student ? data.student.name : 'Unknown card';
    var when = document.createElement('span');
    when.className = 'when';
    when.textContent = data.checkedAt;
    li.appendChild(badge); li.appendChild(who); li.appendChild(when);
    history.insertBefore(li, history.firstChild);
    while (history.children.length > 12) history.removeChild(history.lastChild);
  }

  /* ---------- sound and flash ---------- */
  var audio = null;
  function feedback(tone) {
    viewfinder.classList.remove('flash-pass', 'flash-fail');
    void viewfinder.offsetWidth;
    viewfinder.classList.add(tone === 'pass' ? 'flash-pass' : 'flash-fail');
    if (navigator.vibrate) navigator.vibrate(tone === 'pass' ? 60 : [120, 80, 120]);
    if (!soundBox.checked) return;
    try {
      audio = audio || new (window.AudioContext || window.webkitAudioContext)();
      var notes = tone === 'pass' ? [[880, 0, .12]] : [[260, 0, .18], [200, .22, .22]];
      notes.forEach(function (n) {
        var o = audio.createOscillator(), g = audio.createGain();
        o.frequency.value = n[0]; o.type = tone === 'pass' ? 'sine' : 'square';
        g.gain.setValueAtTime(.0001, audio.currentTime + n[1]);
        g.gain.exponentialRampToValueAtTime(.18, audio.currentTime + n[1] + .02);
        g.gain.exponentialRampToValueAtTime(.0001, audio.currentTime + n[1] + n[2]);
        o.connect(g); g.connect(audio.destination);
        o.start(audio.currentTime + n[1]); o.stop(audio.currentTime + n[1] + n[2] + .05);
      });
    } catch (e) { /* sound not available */ }
  }
})();
