package co.navdeep.kafkaer.utils;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;

import java.util.*;

public class Utils {

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
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        config.put(AdminClientConfig.CLIENT_ID_CONFIG, "kafkaer");
        return config;
    }

    public static org.apache.kafka.clients.admin.Config configsAsKafkaConfig(Map<String, String> config){
        List<ConfigEntry> configEntries = new ArrayList<>();
        for(String key : config.keySet()){
            configEntries.add(new ConfigEntry(key, config.get(key)));
        }
        return new Config(configEntries);
    }
}
