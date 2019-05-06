import co.navdeep.kafkaer.Configurator;
import co.navdeep.kafkaer.model.Config;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ConfiguratorTest {
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
}
