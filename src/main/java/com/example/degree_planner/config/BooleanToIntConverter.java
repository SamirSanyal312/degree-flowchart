package com.example.degree_planner.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class BooleanToIntConverter implements Converter<Boolean, Integer> {
    @Override public Integer convert(Boolean source) {
        return (source != null && source) ? 1 : 0;
    }
}