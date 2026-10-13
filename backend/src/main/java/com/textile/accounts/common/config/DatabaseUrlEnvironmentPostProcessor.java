package com.textile.accounts.common.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Automatically detects and parses cloud database connection strings (e.g. Render, Railway, Neon, Heroku)
 * supplied via DATABASE_URL or SPRING_DATASOURCE_URL in the format:
 *   postgres://user:password@host:port/dbname
 *
 * Converts it to standard JDBC properties:
 *   spring.datasource.url = jdbc:postgresql://host:port/dbname
 *   spring.datasource.username = user
 *   spring.datasource.password = password
 *   spring.flyway.url = jdbc:postgresql://host:port/dbname
 *   spring.flyway.user = user
 *   spring.flyway.password = password
 *
 * Runs with Ordered.LOWEST_PRECEDENCE so it runs after all configuration files (application-prod.yml)
 * are loaded, and adds a MapPropertySource at index 0 (addFirst) so resolved cloud credentials
 * take highest precedence over YAML and raw environment variables.
 *
 * Also protects against unexpanded literal placeholders like "${SPRING_DATASOURCE_USERNAME}".
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> targetProps = new HashMap<>();

        String rawUrl = environment.getProperty("DATABASE_URL");
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = environment.getProperty("SPRING_DATASOURCE_URL");
        }

        if (rawUrl != null && !rawUrl.isBlank()) {
            parseAndApplyDatabaseUrl(rawUrl.trim(), targetProps);
        }

        // Guard against unexpanded literal placeholders entered in cloud dashboards
        sanitizePlaceholder(environment, targetProps, "spring.datasource.username", "SPRING_DATASOURCE_USERNAME", "POSTGRES_USER", "postgres");
        sanitizePlaceholder(environment, targetProps, "spring.datasource.password", "SPRING_DATASOURCE_PASSWORD", "POSTGRES_PASSWORD", "postgres");
        sanitizePlaceholder(environment, targetProps, "spring.datasource.url", "SPRING_DATASOURCE_URL", "POSTGRES_URL", null);

        if (!targetProps.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("cloudDatabasePostProcessor", targetProps));
        }
    }

    private void parseAndApplyDatabaseUrl(String rawUrl, Map<String, Object> targetProps) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return;
        }

        String toParse = rawUrl.trim();

        // If it starts with jdbc: but contains credentials (e.g. jdbc:postgresql://user:pass@host/db),
        // org.postgresql.Driver rejects credentials in the URL authority.
        // We must strip "jdbc:" and treat it as a standard URI so credentials are extracted
        // and the clean jdbcUrl without credentials in the host is produced.
        if (toParse.startsWith("jdbc:")) {
            if (!toParse.contains("@")) {
                targetProps.put("spring.datasource.url", toParse);
                targetProps.put("spring.flyway.url", toParse);
                targetProps.put("SPRING_DATASOURCE_URL", toParse);
                targetProps.put("SPRING_FLYWAY_URL", toParse);
                return;
            }
            if (toParse.startsWith("jdbc:postgresql://")) {
                toParse = "postgresql://" + toParse.substring("jdbc:postgresql://".length());
            } else if (toParse.startsWith("jdbc:postgres://")) {
                toParse = "postgresql://" + toParse.substring("jdbc:postgres://".length());
            } else {
                toParse = toParse.substring("jdbc:".length());
            }
        }

        if (toParse.startsWith("postgres://") || toParse.startsWith("postgresql://")) {
            String cleanUriString = toParse;
            if (cleanUriString.startsWith("postgres://")) {
                cleanUriString = "postgresql://" + cleanUriString.substring("postgres://".length());
            }

            String host = null;
            int port = 5432;
            String path = null;
            String query = null;
            String username = null;
            String password = null;

            try {
                URI uri = URI.create(cleanUriString);
                host = uri.getHost();
                port = uri.getPort() > 0 ? uri.getPort() : 5432;
                path = uri.getPath();
                query = uri.getQuery();

                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isBlank()) {
                    String[] parts = userInfo.split(":", 2);
                    username = decode(parts[0]);
                    if (parts.length > 1) {
                        password = decode(parts[1]);
                    }
                }
            } catch (Exception ignored) {
            }

            // If URI parsing produced a null/blank host (common with unencoded @ in password), invoke fallback parser
            if (host == null || host.isBlank() || path == null || path.isBlank()) {
                ParsedCredentials parsed = parseWithFallback(cleanUriString);
                if (parsed != null) {
                    host = parsed.host;
                    port = parsed.port;
                    path = parsed.path;
                    query = parsed.query;
                    username = parsed.username;
                    password = parsed.password;
                }
            }

            if (host != null && path != null) {
                if (query != null) {
                    query = query.replace("channel_binding=", "channelBinding=");
                }
                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + (query != null && !query.isBlank() ? "?" + query : "");
                targetProps.put("spring.datasource.url", jdbcUrl);
                targetProps.put("spring.flyway.url", jdbcUrl);
                targetProps.put("SPRING_DATASOURCE_URL", jdbcUrl);
                targetProps.put("SPRING_FLYWAY_URL", jdbcUrl);

                if (username != null) {
                    targetProps.put("spring.datasource.username", username);
                    targetProps.put("spring.flyway.user", username);
                    targetProps.put("SPRING_DATASOURCE_USERNAME", username);
                    targetProps.put("SPRING_FLYWAY_USER", username);
                }
                if (password != null) {
                    targetProps.put("spring.datasource.password", password);
                    targetProps.put("spring.flyway.password", password);
                    targetProps.put("SPRING_DATASOURCE_PASSWORD", password);
                    targetProps.put("SPRING_FLYWAY_PASSWORD", password);
                }
            }
        }
    }

    private ParsedCredentials parseWithFallback(String cleanUriString) {
        try {
            // Remove scheme: postgresql://
            int schemeEnd = cleanUriString.indexOf("://");
            if (schemeEnd < 0) return null;
            String withoutScheme = cleanUriString.substring(schemeEnd + 3);

            // Separate path/query from authority
            int slashIdx = withoutScheme.indexOf('/');
            String authority = slashIdx >= 0 ? withoutScheme.substring(0, slashIdx) : withoutScheme;
            String pathAndQuery = slashIdx >= 0 ? withoutScheme.substring(slashIdx) : "/";

            String path = pathAndQuery;
            String query = null;
            int qIdx = pathAndQuery.indexOf('?');
            if (qIdx >= 0) {
                path = pathAndQuery.substring(0, qIdx);
                query = pathAndQuery.substring(qIdx + 1);
            }

            // In authority, userInfo is before the LAST '@'
            int lastAt = authority.lastIndexOf('@');
            String username = null;
            String password = null;
            String hostPort = authority;

            if (lastAt >= 0) {
                String userInfo = authority.substring(0, lastAt);
                hostPort = authority.substring(lastAt + 1);
                String[] userPass = userInfo.split(":", 2);
                username = decode(userPass[0]);
                if (userPass.length > 1) {
                    password = decode(userPass[1]);
                }
            }

            String host = hostPort;
            int port = 5432;
            int colonIdx = hostPort.lastIndexOf(':');
            if (colonIdx >= 0) {
                host = hostPort.substring(0, colonIdx);
                try {
                    port = Integer.parseInt(hostPort.substring(colonIdx + 1));
                } catch (NumberFormatException ignored) {
                    port = 5432;
                }
            }

            return new ParsedCredentials(host, port, path, query, username, password);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String decode(String val) {
        if (val == null) return null;
        try {
            return URLDecoder.decode(val, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return val;
        }
    }

    private void sanitizePlaceholder(ConfigurableEnvironment env, Map<String, Object> targetProps,
                                     String relaxedKey, String envKey, String fallbackEnvKey, String defaultFallback) {
        boolean relaxedInvalid = isPlaceholder(env, relaxedKey);
        boolean envInvalid = isPlaceholder(env, envKey);

        if (relaxedInvalid || envInvalid) {
            String resolvedFallback = resolveFallback(env, fallbackEnvKey, defaultFallback);
            if (resolvedFallback != null) {
                targetProps.put(relaxedKey, resolvedFallback);
                targetProps.put(envKey, resolvedFallback);
            }
        }
    }

    private boolean isPlaceholder(ConfigurableEnvironment env, String key) {
        try {
            String val = env.getProperty(key);
            return val != null && val.trim().startsWith("${") && val.trim().endsWith("}");
        } catch (Exception e) {
            // E.g. PlaceholderResolutionException: Could not resolve placeholder
            return true;
        }
    }

    private String resolveFallback(ConfigurableEnvironment env, String fallbackEnvKey, String defaultFallback) {
        if (fallbackEnvKey != null) {
            try {
                String fallbackVal = env.getProperty(fallbackEnvKey);
                if (fallbackVal != null && !fallbackVal.isBlank() && !fallbackVal.trim().startsWith("${")) {
                    return fallbackVal;
                }
            } catch (Exception ignored) {
            }
        }
        return defaultFallback;
    }

    private static class ParsedCredentials {
        final String host;
        final int port;
        final String path;
        final String query;
        final String username;
        final String password;

        ParsedCredentials(String host, int port, String path, String query, String username, String password) {
            this.host = host;
            this.port = port;
            this.path = path;
            this.query = query;
            this.username = username;
            this.password = password;
        }
    }
}
