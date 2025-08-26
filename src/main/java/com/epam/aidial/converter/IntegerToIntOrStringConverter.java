package com.epam.aidial.converter;

import io.kubernetes.client.custom.IntOrString;
import org.springframework.core.convert.converter.Converter;

public class IntegerToIntOrStringConverter implements Converter<Integer, IntOrString> {
    @Override
    public IntOrString convert(Integer source) {
        return new IntOrString(source);
    }
}