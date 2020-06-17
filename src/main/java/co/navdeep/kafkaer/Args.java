package co.navdeep.kafkaer;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

import java.util.ArrayList;
import java.util.List;

@Data
public class Args {
    @Option(name="--config",aliases = "-c", usage="Location of config file")
    String config;

    @Option(name="--properties", aliases = "-p", usage="Location of properties file")
    String properties;

    @Option(name="--wipe", usage="Wipe all topics", handler = BooleanOptionHandler.class)
    boolean wipe;

    @Option(name="--help", aliases= "-h", help = true, usage="list usage", handler =  BooleanOptionHandler.class)
    boolean help;

    @Option(name="--debug", aliases = "-d", usage = "debug mode", handler = BooleanOptionHandler.class)
    boolean debug;

    @Argument
    private List<String> arguments = new ArrayList<>();

    //Maintain backward compatibility for use without flags
    public String getConfig(){
        return StringUtils.isBlank(config) ? arguments.size() < 2 ? null : arguments.get(1) : config;
    }

    public String getProperties(){
        return StringUtils.isBlank(properties) ? arguments.size() < 1 ? null : arguments.get(0) : properties;
    }
}
