package com.redhat.fuse.commands.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.exec.ExecResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class GetPortProcessor implements Processor {

    private static String TARGET_LINE = "SSH Url";

    @Override
    public void process(Exchange exchange) throws Exception {
        ExecResult body = (ExecResult) exchange.getIn().getBody();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(body.getStdout()));

        String line;
        String port = null;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.contains(TARGET_LINE)) {
                String[] tokens = line.split(":");
                port = tokens[tokens.length-1];

                break;
            }
        }

        exchange.getIn().setHeader("port", port);
    }
}
