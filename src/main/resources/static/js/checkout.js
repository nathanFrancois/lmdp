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
    var shippingFee = parseFloat(form.getAttribute('data-shipping-fee')) || 0;
    var itemsTotal = 0;

    function selectedDeliveryMethod() {
        var checked = form.querySelector('input[name="deliveryMethod"]:checked');
        return checked ? checked.value : 'PICKUP';
    }

    function refreshTotals() {
        var shipping = selectedDeliveryMethod() === 'SHIPPING' ? shippingFee : 0;
        totalLabel.textContent = cart.formatPrice(itemsTotal + shipping) + ' €';
        addressFields.hidden = shipping === 0;
    }

    function renderEmptyCart() {
        summary.innerHTML = '<p>Votre panier est vide. <a href="/creations">Découvrir les créations</a></p>';
        submitButton.disabled = true;
        totalLabel.textContent = '—';
    }

    function renderLines(lines) {
        var html = '<ul class="checkout-lines">';
        lines.forEach(function (line) {
            itemsTotal += line.lineTotal;
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
        refreshTotals();
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

