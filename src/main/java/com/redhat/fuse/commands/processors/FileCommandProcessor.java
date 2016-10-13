package com.redhat.fuse.commands.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.io.*;

public class FileCommandProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        String commandId = (String) exchange.getIn().getHeader("command");
        String sourcePath = (String) exchange.getIn().getHeader("sourcePath");

        File file = new File(sourcePath + "/" + commandId);

        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        String target = bufferedReader.readLine();
        String command = bufferedReader.readLine();

        if (this.isContainerPort(target)) {
            // Port is present, set it as header and continue
            exchange.getOut().setHeader("target", "port");
            exchange.getOut().setHeader("port", getPortValue(target));

        } else {
            // The container name is present, set it as header for further processing
            exchange.getOut().setHeader("target", "container");
            exchange.getOut().setHeader("container", getPortValue(target));
        }

        exchange.getOut().setHeader("command", getCommandValue(command));
    }

    private String getPortValue(String line) {
        return line.split(":")[1].trim();
    }

    private String getCommandValue(String line) {
        return line.substring(line.indexOf(":") + 1).trim();
    }

    private boolean isContainerPort(String target) {
        if (target.contains("port"))
            return true;
        return false;
    }
}
