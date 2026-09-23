// Menu mobile : ouverture/fermeture accessible au clavier et à la souris.
(function () {
  var toggle = document.getElementById('nav-toggle');
  var nav = document.getElementById('main-nav');

  if (!toggle || !nav) {
    return;
  }

  toggle.addEventListener('click', function () {
    var isOpen = nav.classList.toggle('is-open');
    toggle.setAttribute('aria-expanded', String(isOpen));
  });
})();

