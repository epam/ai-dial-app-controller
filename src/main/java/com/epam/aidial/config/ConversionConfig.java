package com.epam.aidial.config;

import com.epam.aidial.converter.IntegerToIntOrStringConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.GenericConversionService;

@Configuration
public class ConversionConfig {
    @Bean
    public GenericConversionService conversionService() {
        GenericConversionService conversionService = new GenericConversionService();
        conversionService.addConverter(new IntegerToIntOrStringConverter());
        return conversionService;
    }
}