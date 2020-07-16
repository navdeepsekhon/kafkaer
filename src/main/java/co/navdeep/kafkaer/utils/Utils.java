package co.navdeep.kafkaer.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Utils {
    private static final String KAFKAER = "kafkaer";
    private static final String KAFKAER_DOT = KAFKAER + ".";

    public static final String MAX_DELETE_CONFIRM_WAIT_CONFIG = "kafkaer.max.delete.confirm.wait";
    public static final String SCHEMA_REGISTRY_URL_CONFIG = KAFKAER_DOT +  "schema.registry.url";

    private static final String SCHEMA_REGISTRY_CONFIG_PREFIX = "schema.registry";
    private static final String SCHEMA_REGISTRY_CONFIG_PREFIX_DOT = SCHEMA_REGISTRY_CONFIG_PREFIX + ".";
    private static final String KAFKAER_SCHEMA_REGISTRY_CONFIG_PREFIX = KAFKAER_DOT + SCHEMA_REGISTRY_CONFIG_PREFIX;
    private static final String KAFKAER_SCHEMA_REGISTRY_CONFIG_PREFIX_DOT = KAFKAER_SCHEMA_REGISTRY_CONFIG_PREFIX + ".";

    public static Configuration readProperties(String location) throws ConfigurationException {
        return new Configurations().properties(location);
    }

    public static Map<String, String> readPropertiesAsMap(String location) throws ConfigurationException {
        return propertiesToMap(readProperties(location));
    }

    public static Map<String, String> propertiesToMap(Configuration properties){
        Map<String, String> map = new HashMap<>();
        properties.getKeys().forEachRemaining(s -> map.put(s, properties.getString(s)));
        return map;
    }

    public static Properties getClientConfig(Configuration properties){
        Properties config = new Properties();
        properties.getKeys(KAFKAER).forEachRemaining(key -> config.put(replacePrefix(key, KAFKAER_DOT, null), properties.getString(key)));
        return config;
    }

    public static Map<String, String> getSchemaRegistryConfigs(Configuration properties){
        Map<String, String> config = new HashMap<>();
        properties.getKeys(KAFKAER_SCHEMA_REGISTRY_CONFIG_PREFIX).forEachRemaining(key -> config.put(replacePrefix(key, KAFKAER_SCHEMA_REGISTRY_CONFIG_PREFIX_DOT, SCHEMA_REGISTRY_CONFIG_PREFIX_DOT), properties.getString(key)));
        return config;
    }

    public static int getMaxDeleteConfirmWaitTime(Configuration properties){
        return properties.getInt(MAX_DELETE_CONFIRM_WAIT_CONFIG, 60);
    }

    private static String replacePrefix(String key, String prefix, String replaceWith){
        String stripped = key.substring(key.indexOf(prefix) + prefix.length());
        return StringUtils.isBlank(replaceWith) ? stripped : (replaceWith + stripped);
    }

    public static org.apache.kafka.clients.admin.Config configsAsKafkaConfig(Map<String, String> config){
        List<ConfigEntry> configEntries = new ArrayList<>();
        for(String key : config.keySet()){
            configEntries.add(new ConfigEntry(key, config.get(key)));
        }
        return new Config(configEntries);
    }

    public static co.navdeep.kafkaer.model.Config readConfig(String location, Map<String, String> valueMap) throws IOException {
        String configString = FileUtils.readFileToString(new File(location), UTF_8);
        StringSubstitutor substitutor = new StringSubstitutor(valueMap);
        configString = substitutor.replace(configString);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(configString, co.navdeep.kafkaer.model.Config.class);
    }

    public static String getSchemaRegistryUrl(Configuration properties){
        return properties.getString(SCHEMA_REGISTRY_URL_CONFIG);
    }
}
