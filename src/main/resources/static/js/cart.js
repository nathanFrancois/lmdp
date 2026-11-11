// Panier côté client (localStorage) — pas de compte, pas de session serveur.
// On ne stocke que des identifiants de produit + quantités : le nom, le prix et la
// disponibilité sont toujours relus depuis l'API /api/produits (source de vérité),
// et le total facturé est recalculé par le serveur au moment de la commande.
(function (window) {
    var STORAGE_KEY = 'lmdp-panier';

    function getCart() {
        try {
            return JSON.parse(localStorage.getItem(STORAGE_KEY)) || {};
        } catch (e) {
            return {};
        }
    }

    function saveCart(cart) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(cart));
    }

    function clearCart() {
        localStorage.removeItem(STORAGE_KEY);
    }

    function isEmpty() {
        return Object.keys(getCart()).length === 0;
    }

    function formatPrice(value) {
        return value.toFixed(2).replace('.', ',');
    }

    // Format attendu par le serveur : "identifiant:quantite,identifiant:quantite".
    function serialize() {
        var cart = getCart();
        return Object.keys(cart).map(function (id) { return id + ':' + cart[id]; }).join(',');
    }

    function fetchProducts(url) {
        var cart = getCart();
        var ids = Object.keys(cart);
        if (ids.length === 0) return Promise.resolve([]);
        var params = new URLSearchParams();
        ids.forEach(function (id) { params.append('ids', id); });
        return fetch(url + (url.indexOf('?') === -1 ? '?' : '&') + params.toString(), {cache: 'no-store'})
            .then(function (response) {
                if (!response.ok) throw new Error('Chargement du panier impossible');
                return response.json();
            })
            .then(function (products) {
                return products
                    .filter(function (p) { return cart[p.id]; })
                    .map(function (p) {
                        var quantity = cart[p.id];
                        return { product: p, quantity: quantity, lineTotal: p.price * quantity };
                    });
            });
    }

    function escapeHtml(value) {
        return String(value).replace(/[&<>"']/g, function (char) {
            return {'&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'}[char];
        });
    }

    // API partagée avec les autres scripts de la boutique.
    window.LmdpCart = {
        get: getCart,
        save: saveCart,
        clear: clearCart,
        isEmpty: isEmpty,
        serialize: serialize,
        formatPrice: formatPrice,
        fetchLines: fetchProducts
    };

    function updateBadge() {
        var badge = document.getElementById('cart-badge');
        if (!badge) return;
        var cart = getCart();
        var count = Object.keys(cart).reduce(function (sum, id) { return sum + cart[id]; }, 0);
        badge.hidden = count === 0;
        badge.textContent = count;
    }

    // --- Fiche produit : seul endroit où l'on ajoute au panier ---
    var addButton = document.getElementById('add-to-cart-btn');
    if (addButton) {
        addButton.addEventListener('click', function () {
            var quantityInput = document.getElementById('quantity');
            var quantity = Math.max(1, parseInt(quantityInput ? quantityInput.value : 1, 10) || 1);
            var cart = getCart();
            var id = addButton.getAttribute('data-product-id');
            cart[id] = (cart[id] || 0) + quantity;
            saveCart(cart);
            window.location.href = '/panier';
        });
    }

    // --- Page de confirmation : le panier n'est vidé qu'après paiement confirmé par le serveur ---
    var orderView = document.getElementById('order-view');
    if (orderView && orderView.getAttribute('data-clear-cart') === 'true') {
        clearCart();
    }

    // --- Page panier : affichage et gestion des lignes ---
    var app = document.getElementById('cart-app');
    var cachedProducts = [];
    function renderCart() {
        if (!app) return;
        if (isEmpty()) {
            app.innerHTML = '<p>Votre panier est vide. <a href="/creations">Découvrir les créations</a></p>';
        } else {
            var cart = getCart();
            var lines = cachedProducts.filter(function (p) { return cart[p.id]; }).map(function (p) {
                return {product: p, quantity: cart[p.id], lineTotal: p.price * cart[p.id]};
            });
            if (!lines.length) {
                app.innerHTML = '<p>Les articles de votre panier ne sont plus disponibles. <a href="/creations">Voir les créations</a></p>';
                return;
            }
            var total = 0;
            var html = '<ul class="cart-list">';

            lines.forEach(function (line) {
                var p = line.product;
                total += line.lineTotal;
                var imageHtml = p.imageUrl
                    ? '<img src="' + escapeHtml(p.imageUrl) + '" alt="' + escapeHtml(p.name) + '" class="product-photo" loading="lazy" decoding="async">'
                    : '<div class="product-image-placeholder" role="img" aria-label="Photo non disponible pour ' + escapeHtml(p.name) + '"><span aria-hidden="true">Photo à venir</span></div>';
                html += '<li class="cart-line">'
                    + '<div class="cart-line-image">' + imageHtml + '</div>'
                    + '<div class="cart-line-info"><h2 class="cart-line-name">' + escapeHtml(p.name) + '</h2>'
                    + '<p class="cart-line-unit-price">' + formatPrice(p.price) + '&nbsp;€ / unité</p></div>'
                    + '<input type="number" min="1" value="' + line.quantity + '" class="qty-input js-qty" data-id="' + escapeHtml(p.id) + '">'
                    + '<p class="cart-line-total">' + formatPrice(line.lineTotal) + '&nbsp;€</p>'
                    + '<button type="button" class="cart-remove-btn js-remove" data-id="' + escapeHtml(p.id) + '">Retirer</button>'
                    + '</li>';
            });

            html += '</ul><div class="cart-summary">'
                + '<p class="cart-total">Total : <strong>' + formatPrice(total) + '&nbsp;€</strong></p>'
                + '<div class="cart-summary-actions">'
                + '<button type="button" id="clear-cart" class="btn btn-outline">Vider le panier</button>'
                + '<a href="/commande" class="btn btn-primary">Passer commande</a>'
                + '</div>'
                + '<p><a href="/creations" class="link-arrow">← Continuer mes achats</a></p>'
                + '</div>';

            app.innerHTML = html;

            app.querySelectorAll('.js-qty').forEach(function (input) {
                input.addEventListener('change', function () {
                    var c = getCart();
                    var value = parseInt(input.value, 10);
                    if (!value || value < 1) {
                        delete c[input.getAttribute('data-id')];
                    } else {
                        c[input.getAttribute('data-id')] = value;
                    }
                    saveCart(c);
                    renderCart();
                    updateBadge();
                });
            });

            app.querySelectorAll('.js-remove').forEach(function (button) {
                button.addEventListener('click', function () {
                    var c = getCart();
                    delete c[button.getAttribute('data-id')];
                    saveCart(c);
                    renderCart();
                    updateBadge();
                });
            });

            var clearButton = document.getElementById('clear-cart');
            if (clearButton) {
                clearButton.addEventListener('click', function () {
                    clearCart();
                    renderCart();
                    updateBadge();
                });
            }
        }
    }

    if (app) {
        if (isEmpty()) renderCart();
        else fetchProducts(app.getAttribute('data-products-url')).then(function (lines) {
            cachedProducts = lines.map(function (line) { return line.product; });
            renderCart();
        }).catch(function () {
            app.innerHTML = '<p>Le panier n’a pas pu être chargé. <button type="button" id="retry-cart">Réessayer</button></p>';
            document.getElementById('retry-cart').addEventListener('click', function () { window.location.reload(); });
        });
    }

    updateBadge();
})(window);
