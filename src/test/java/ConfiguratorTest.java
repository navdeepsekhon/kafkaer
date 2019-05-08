import co.navdeep.kafkaer.Configurator;
import co.navdeep.kafkaer.model.Config;
import co.navdeep.kafkaer.utils.Utils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.kafka.clients.admin.AdminClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ConfiguratorTest {

    static AdminClient adminClient;

    @BeforeClass
    public static void setup() throws ConfigurationException {
        Configuration properties = Utils.readProperties("src/test/resources/test.properties");
        adminClient = AdminClient.create(Utils.getClientConfig(properties));
    }

    @Test
    public void testReadConfig() throws IOException, ConfigurationException {
        Configurator configurator = new Configurator("src/test/resources/test.properties", "src/test/resources/kafka-config.json");
        Config config = configurator.getConfig();
        Assert.assertFalse(config.getTopics().isEmpty());
        Assert.assertEquals(config.getTopics().get(0).getName(), "withSuffix-iamasuffix");
        Assert.assertEquals(config.getTopics().get(0).getPartitions(), 3);
        Assert.assertEquals(config.getTopics().get(0).getReplicationFactor(), 3);
        Assert.assertEquals(config.getTopics().get(0).getConfigs().get("compression.type"), "gzip");
    }

    @Test
    public void testTopicCreation() throws ExecutionException, InterruptedException, IOException, ConfigurationException {
        Configurator configurator = new Configurator("src/test/resources/test.properties", "src/test/resources/kafka-config.json");
        configurator.applyConfig();
    }
}
