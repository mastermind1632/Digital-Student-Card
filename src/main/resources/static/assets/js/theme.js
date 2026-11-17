/* Applies the saved light/dark choice before the page paints, so it never flashes the wrong theme. */
(function () {
  try {
    var t = localStorage.getItem('dsc-theme');
    if (t === 'light' || t === 'dark') document.documentElement.setAttribute('data-theme', t);
  } catch (e) { /* storage blocked: follow the system setting */ }
})();
