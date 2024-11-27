package com.epam.aidial.util.mapping;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public record FieldMapper<T, Y>(
        Supplier<Y> factory,
        Function<T, Y> getter,
        BiConsumer<T, Y> setter) {
    public Y getOrSet(T object) {
        Y property = getter.apply(object);
        if (property == null) {
            property = factory.get();
            setter.accept(object, property);
        }

        return property;
    }
}
