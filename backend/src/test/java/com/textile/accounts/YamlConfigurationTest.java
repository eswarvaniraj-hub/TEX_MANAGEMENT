package com.textile.accounts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class YamlConfigurationTest {

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @ParameterizedTest
    @ValueSource(strings = {"application.yml", "application-dev.yml", "application-prod.yml"})
    @DisplayName("Verify each application YAML file is well-formed and parseable by Spring's YamlPropertySourceLoader")
    void yamlFileLoadsSuccessfully(String filename) throws IOException {
        ClassPathResource resource = new ClassPathResource(filename);
        assertThat(resource.exists())
                .as("Resource %s should exist on classpath", filename)
                .isTrue();

        List<PropertySource<?>> propertySources = loader.load(filename, resource);
        assertThat(propertySources)
                .as("Parsed property sources for %s should not be empty", filename)
                .isNotEmpty();
    }
}
