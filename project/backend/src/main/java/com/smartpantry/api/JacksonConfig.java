package com.smartpantry.api;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;

/**
 * Strict number binding for the request body (REQ-029). A decimal such as 1.5
 * is not truncated to 1, and a quoted number such as "3" is not accepted for an
 * integer field. Both are reported as binding errors on the field.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer strictIntegerBinding() {
        return builder -> builder
                .featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .postConfigurer(mapper -> mapper.coercionConfigFor(LogicalType.Integer)
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail));
    }
}
