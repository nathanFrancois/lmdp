// Page de commande : récapitulatif du panier, affichage conditionnel de l'adresse
// de livraison et transmission du panier au serveur (identifiants + quantités).
// Le total affiché ici est purement indicatif : le montant facturé est recalculé
// côté serveur à partir du catalogue.
(function () {
    var form = document.getElementById('checkout-form');
    if (!form || !window.LmdpCart) return;

    var cart = window.LmdpCart;
    var cartField = document.getElementById('cart-field');
    var summary = document.getElementById('checkout-summary-lines');
    var totalLabel = document.getElementById('checkout-total');
    var submitButton = document.getElementById('checkout-submit');
    var addressFields = document.getElementById('shipping-address-fields');
    var shippingThree = Number(form.getAttribute('data-shipping-three'));
    var shippingFive = Number(form.getAttribute('data-shipping-five'));
    var shippingMore = Number(form.getAttribute('data-shipping-more'));
    var itemCount = 0;
    var loaded = false;
    var itemsTotal = 0;

    function selectedDeliveryMethod() {
        var checked = form.querySelector('input[name="deliveryMethod"]:checked');
        return checked ? checked.value : 'PICKUP';
    }

    function refreshTotals() {
        var isShipping = selectedDeliveryMethod() === 'SHIPPING';
        var shippingFee = itemCount <= 3 ? shippingThree : itemCount <= 5 ? shippingFive : shippingMore;
        var shipping = isShipping ? shippingFee : 0;
        totalLabel.textContent = loaded ? cart.formatPrice(itemsTotal + shipping) + ' €' : '—';
        addressFields.hidden = !isShipping;
        addressFields.disabled = !isShipping;
        ['street', 'postalCode', 'city'].forEach(function (id) {
            document.getElementById(id).required = isShipping;
        });
        document.getElementById('delivery-price').textContent = loaded ? cart.formatPrice(shippingFee) + ' €' : 'Selon le panier';
        document.getElementById('checkout-delivery-label').textContent = isShipping ? 'Livraison · ' + itemCount + ' article(s)' : 'Remise en main propre';
        document.getElementById('checkout-shipping').textContent = !loaded ? '—' : isShipping ? cart.formatPrice(shipping) + ' €' : 'Gratuite';
        document.getElementById('checkout-delivery-note').textContent = isShipping
            ? 'Livraison en France métropolitaine.'
            : 'Canton de Genève et Chablais (74) uniquement. L’artisan vous contactera après validation pour fixer le rendez-vous.';
    }

    function renderEmptyCart() {
        summary.innerHTML = '<p>Votre panier est vide. <a href="/creations">Découvrir les créations</a></p>';
        submitButton.disabled = true;
        totalLabel.textContent = '—';
    }

    function renderLines(lines) {
        itemsTotal = 0;
        itemCount = 0;
        var html = '<ul class="checkout-lines">';
        lines.forEach(function (line) {
            itemsTotal += line.lineTotal;
            itemCount += Number(line.quantity);
            html += '<li class="checkout-line">'
                + '<span class="checkout-line-name">' + line.product.name + '</span>'
                + '<span class="checkout-line-qty">× ' + line.quantity + '</span>'
                + '<span class="checkout-line-total">' + cart.formatPrice(line.lineTotal) + '&nbsp;€</span>'
                + '</li>';
        });
        html += '</ul>';
        summary.innerHTML = html;
    }

    if (cart.isEmpty()) {
        renderEmptyCart();
        return;
    }

    cartField.value = cart.serialize();

    cart.fetchLines(form.getAttribute('data-products-url')).then(function (lines) {
        if (lines.length === 0) {
            renderEmptyCart();
            return;
        }
        renderLines(lines);
        loaded = true;
        submitButton.disabled = false;
        refreshTotals();
    }).catch(function () {
        summary.textContent = 'Le panier n’a pas pu être chargé. Actualisez la page pour réessayer.';
        submitButton.disabled = true;
    });

    form.querySelectorAll('input[name="deliveryMethod"]').forEach(function (input) {
        input.addEventListener('change', refreshTotals);
    });

    // Le panier peut avoir changé dans un autre onglet : on le resynchronise à l'envoi.
    form.addEventListener('submit', function () {
        cartField.value = cart.serialize();
        submitButton.disabled = true;
        submitButton.textContent = 'Redirection vers le paiement…';
    });

    refreshTotals();
})();

