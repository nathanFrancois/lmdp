const test = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const source = fs.readFileSync('src/main/resources/static/js/cart.js', 'utf8');

function setup(initialCart, withApp = false) {
    let stored = JSON.stringify(initialCart), requests = [], reloads = 0;
    const qty = {value: '2', getAttribute: () => 'vase', addEventListener: (_, fn) => qty.change = fn};
    const remove = {getAttribute: () => 'vase', addEventListener: (_, fn) => remove.click = fn};
    const clear = {addEventListener: (_, fn) => clear.click = fn};
    const app = {innerHTML: '', getAttribute: () => '/api/produits/panier', querySelectorAll: selector => selector === '.js-qty' ? [qty] : [remove]};
    const cartBadge = {};
    const context = {URLSearchParams, Promise,
        document: {getElementById: id => ({'cart-app': withApp ? app : null, 'cart-badge': cartBadge, 'clear-cart': clear})[id]},
        localStorage: {getItem: () => stored, setItem: (_, value) => stored = value, removeItem: () => stored = '{}'},
        window: {location: {reload: () => reloads++}},
        fetch: async (url, options) => {
            requests.push({url, options});
            return {ok: true, json: async () => [{id: 'vase', name: '<Vase>', price: 10, stock: 10, imageUrl: '/api/produits/vase/image'}]};
        }
    };
    vm.runInNewContext(source, context);
    return {context, requests, app, qty, remove, clear, reloads: () => reloads};
}

test('only basket identifiers are requested, fresh prices are used', async () => {
    const app = setup({vase: 3});
    const lines = await app.context.window.LmdpCart.fetchLines('/api/produits/panier');
    assert.equal(app.requests[0].url, '/api/produits/panier?ids=vase');
    assert.equal(app.requests[0].options.cache, 'no-store');
    assert.equal(lines[0].lineTotal, 30);
});
test('empty basket makes no request', async () => {
    const app = setup({});
    assert.equal((await app.context.window.LmdpCart.fetchLines('/api/produits/panier')).length, 0);
    assert.equal(app.requests.length, 0);
});
test('quantity updates and removal do not reload or refetch catalogue', async () => {
    const app = setup({vase: 1}, true);
    await new Promise(resolve => setImmediate(resolve));
    assert.match(app.app.innerHTML, /&lt;Vase&gt;/);
    app.qty.change();
    assert.match(app.app.innerHTML, /20,00/);
    app.remove.click();
    assert.match(app.app.innerHTML, /Votre panier est vide/);
    assert.equal(app.requests.length, 1);
    assert.equal(app.reloads(), 0);
});
