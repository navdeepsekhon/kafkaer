import co.navdeep.kafkaer.utils.Utils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.Config;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class UtilsTest {
    @Test
    public void getClientConfigsTest() throws ConfigurationException {
        Configuration properties = Utils.readProperties("src/test/resources/test.properties");
        Properties config = Utils.getClientConfig(properties);
        Assert.assertEquals(config.size(), 2);
        Assert.assertEquals(config.getProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG), "localhost:9092");
        Assert.assertEquals(config.getProperty(AdminClientConfig.CLIENT_ID_CONFIG), "kafkaer");
    }

    @Test
    public void readPropertiesTest() throws ConfigurationException {
        Configuration properties = Utils.readProperties("src/test/resources/test.properties");
        Assert.assertEquals(properties.getString("kafkaer.bootstrap.servers"), "localhost:9092");
        Assert.assertEquals(properties.getString("topic.suffix"), "iamasuffix");
        Assert.assertEquals(properties.getString("kafkaer.client.id"), "kafkaer");
    }

    @Test
    public void propertiesToMapTest() throws ConfigurationException {
        Configuration properties = Utils.readProperties("src/test/resources/test.properties");
        Map<String, String> map = Utils.propertiesToMap(properties);
        Assert.assertEquals(map.get("kafkaer.bootstrap.servers"), "localhost:9092");
        Assert.assertEquals(map.get("topic.suffix"), "iamasuffix");
        Assert.assertEquals(map.get("kafkaer.client.id"), "kafkaer");
    }

    @Test
    public void readPropertiesAsMapTest() throws ConfigurationException {
        Map<String, String> map = Utils.readPropertiesAsMap("src/test/resources/test.properties");
        Assert.assertEquals(map.get("kafkaer.bootstrap.servers"), "localhost:9092");
        Assert.assertEquals(map.get("topic.suffix"), "iamasuffix");
        Assert.assertEquals(map.get("kafkaer.client.id"), "kafkaer");
    }

    @Test
    public void configsAsKafkaConfigTest(){
        Map<String, String> config = new HashMap<>();
        config.put("config1", "val1");
        config.put("config2", "val2");

        Config kafkaConfig = Utils.configsAsKafkaConfig(config);
        for(String key : config.keySet()){
            Assert.assertEquals(kafkaConfig.get(key).value(), config.get(key));
        }
    }

    @Test
    public void readConfigTest() throws IOException {
        co.navdeep.kafkaer.model.Config config = Utils.readConfig("src/test/resources/kafka-config.json", Collections.singletonMap("topic.suffix", "t"));
        co.navdeep.kafkaer.model.Config config2 = Utils.readConfig("src/test/resources/kafka-config.json", Collections.singletonMap("topic.suffix", "t2"));

        Assert.assertEquals(config.getTopics().get(0).getName(), "withSuffix-t");
        Assert.assertEquals(config2.getTopics().get(0).getName(), "withSuffix-t2");
    }

    @Test
    public void readConfigWithDescriptinoTest() throws IOException {
        co.navdeep.kafkaer.model.Config config = Utils.readConfig("src/test/resources/kafka-config-with-description.json", Collections.singletonMap("topic.suffix", "t"));
        Assert.assertNotNull(config.getTopics().get(0).getDescription());
    }

    @Test
    public void readConfigWithoutDescriptinoTest() throws IOException {
        co.navdeep.kafkaer.model.Config config = Utils.readConfig("src/test/resources/kafka-config.json", Collections.singletonMap("topic.suffix", "t"));
        Assert.assertNull(config.getTopics().get(0).getDescription());
    }

    @Test
    public void getSchemaRegistryUrlConfigTest() {
        Configuration p = new PropertiesConfiguration();
        p.addProperty(Utils.SCHEMA_REGISTRY_URL_CONFIG, "url");
        Assert.assertEquals("url", Utils.getSchemaRegistryUrl(p));
    }

    @Test
    public void getSchemaRegistryConfigsTest(){
        Configuration p = new PropertiesConfiguration();
        p.addProperty("kafkaer.schema.registry.url", "u");
        p.addProperty("x.y", "x");
        Map<String, String> configs = Utils.getSchemaRegistryConfigs(p);
        Assert.assertEquals(1, configs.size());
        Assert.assertEquals("u", configs.get("schema.registry.url"));
    }
}
