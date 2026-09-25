package fr.lmdp.order;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Conversions entre les montants métier (euros) et le format attendu par CAWL (centimes). */
public final class Money {

    private Money() {
    }

    public static long toMinorUnits(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    public static BigDecimal fromMinorUnits(long minorUnits) {
        return BigDecimal.valueOf(minorUnits, 2);
    }
}

