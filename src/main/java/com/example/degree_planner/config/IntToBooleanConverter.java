package com.example.degree_planner.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class IntToBooleanConverter implements Converter<Integer, Boolean> {
    @Override public Boolean convert(Integer source) {
        return source != null && source != 0;
    }
}