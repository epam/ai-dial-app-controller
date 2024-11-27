package com.epam.aidial.util.mapping;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public record NamedItemMapper<T>(
        Supplier<T> factory,
        Function<T, String> getter,
        BiConsumer<T, String> setter) {
}
