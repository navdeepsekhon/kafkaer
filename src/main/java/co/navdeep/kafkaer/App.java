package co.navdeep.kafkaer;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    public static void main(String[] a) throws Exception {
        Args args = new Args();
        CmdLineParser parser = new CmdLineParser(args);
        try {
            parser.parseArgument(a);
        } catch(CmdLineException e){
            throw new Exception("Invalid command line arguments", e);
        }
        if(args.isHelp()){
            parser.printUsage(System.out);
            return;
        }

        if(args.isDebug()){
            System.setProperty("org.slf4j.simpleLogger.log.co.navdeep", "debug");
        }
        Logger logger = LoggerFactory.getLogger(App.class);

        if(args.getProperties() == null || args.getConfig() == null) {
            throw new RuntimeException("Missing required arguments - propertiesLocation, configLocation");
        }

        logger.debug("Input args: config: [{}] properties: [{}] wipe:[{}] confirm-delete: [{}]", args.getConfig(), args.getProperties(), args.isWipe(), args.isConfirmDelete());
        Configurator configurator = new Configurator(args.getProperties(), args.getConfig());
        if(args.isWipe())
            configurator.wipeTopics(args.isConfirmDelete());
        else
            configurator.applyConfig();
    }
}
