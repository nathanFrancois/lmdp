const test = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const source = fs.readFileSync('src/main/resources/static/js/checkout.js', 'utf8');

async function setup(quantities, fail = false) {
    const ids = ['cart-field','checkout-summary-lines','checkout-total','checkout-submit','shipping-address-fields',
        'street','postalCode','city','delivery-price','checkout-delivery-label','checkout-shipping','checkout-delivery-note'];
    const nodes = Object.fromEntries(ids.map(id => [id, {disabled: id === 'checkout-submit'}]));
    let method = 'SHIPPING';
    const handlers = [];
    const form = {
        getAttribute: name => ({'data-shipping-three':'6.90','data-shipping-five':'7.90','data-shipping-more':'9.90'})[name],
        querySelector: () => ({value: method}),
        querySelectorAll: () => [{addEventListener: (_, fn) => handlers.push(fn)}],
        addEventListener: () => {}
    };
    nodes['checkout-form'] = form;
    const cart = {isEmpty: () => false, serialize: () => 'vase:1', formatPrice: n => n.toFixed(2).replace('.', ','),
        fetchLines: () => fail ? Promise.reject(new Error('Network')) : Promise.resolve(quantities.map(quantity => ({quantity, lineTotal: quantity * 10, product: {name: 'Vase'}})))};
    vm.runInNewContext(source, {document: {getElementById: id => nodes[id]}, window: {LmdpCart: cart}});
    await new Promise(resolve => setImmediate(resolve));
    return {nodes, pickup() {method = 'PICKUP'; handlers.forEach(fn => fn());}};
}

for (const [quantities, fee] of [[[1],6.9],[[3],6.9],[[4],7.9],[[2,3],7.9],[[3,3],9.9]]) {
    test(`delivery rate and pickup toggle for quantities ${quantities}`, async () => {
        const app = await setup(quantities);
        const count = quantities.reduce((a,b) => a+b, 0);
        assert.equal(app.nodes['checkout-total'].textContent, (count*10+fee).toFixed(2).replace('.', ',')+' €');
        assert.equal(app.nodes['checkout-shipping'].textContent, fee.toFixed(2).replace('.', ',')+' €');
        assert.equal(app.nodes.street.required, true);
        assert.equal(app.nodes['shipping-address-fields'].hidden, false);
        assert.equal(app.nodes['checkout-submit'].disabled, false);
        app.pickup();
        assert.equal(app.nodes['checkout-total'].textContent, (count*10).toFixed(2).replace('.', ',')+' €');
        assert.equal(app.nodes['checkout-shipping'].textContent, 'Gratuite');
        assert.equal(app.nodes['shipping-address-fields'].disabled, true);
        assert.match(app.nodes['checkout-delivery-note'].textContent, /Genève et Chablais/);
    });
}
test('network error keeps payment disabled', async () => {
    const app = await setup([1], true);
    assert.equal(app.nodes['checkout-submit'].disabled, true);
    assert.match(app.nodes['checkout-summary-lines'].textContent, /Actualisez/);
});
