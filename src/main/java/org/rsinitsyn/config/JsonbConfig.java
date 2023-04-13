package org.rsinitsyn.config;

import io.quarkus.jsonb.JsonbConfigCustomizer;
import javax.inject.Singleton;
import javax.json.bind.config.PropertyOrderStrategy;

@Singleton
public class JsonbConfig implements JsonbConfigCustomizer {
    @Override
    public void customize(javax.json.bind.JsonbConfig jsonbConfig) {
        jsonbConfig.withNullValues(false);
        jsonbConfig.withFormatting(true);
        jsonbConfig.withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL);
    }
}
