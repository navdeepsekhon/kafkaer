package co.navdeep.kafkaer.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Utils {
    private static final String PROPERTY_PREFIX = "kafkaer";

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
        properties.getKeys(PROPERTY_PREFIX).forEachRemaining( key -> config.put(stripPropertyPrefix(key), properties.getString(key)));
        return config;
    }

    private static String stripPropertyPrefix(String key){
        return key.substring(key.indexOf(PROPERTY_PREFIX) + PROPERTY_PREFIX.length() + 1);
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
}
