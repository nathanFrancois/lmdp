package fr.lmdp.order;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    @Test
    void parsesIdentifiersAndQuantities() {
        Cart cart = Cart.parse("vase-olive:2,bougeoir:1");

        assertThat(cart.items())
                .containsExactly(new Cart.CartItem("vase-olive", 2), new Cart.CartItem("bougeoir", 1));
    }

    @Test
    void mergesDuplicatedLines() {
        assertThat(Cart.parse("vase:1,vase:2").items()).containsExactly(new Cart.CartItem("vase", 3));
    }

    @Test
    void rejectsEmptyCart() {
        assertThatThrownBy(() -> Cart.parse("   ")).isInstanceOf(CheckoutException.class);
    }

    @Test
    void rejectsMalformedContent() {
        assertThatThrownBy(() -> Cart.parse("vase")).isInstanceOf(CheckoutException.class);
        assertThatThrownBy(() -> Cart.parse("vase:0")).isInstanceOf(CheckoutException.class);
        assertThatThrownBy(() -> Cart.parse("vase:-3")).isInstanceOf(CheckoutException.class);
        assertThatThrownBy(() -> Cart.parse("vase:beaucoup")).isInstanceOf(CheckoutException.class);
    }
}

