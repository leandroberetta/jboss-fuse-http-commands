package com.redhat.fuse.commands.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.exec.ExecResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResponseProcessor implements Processor {

    private Logger logger = LoggerFactory.getLogger(ResponseProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        ExecResult body = (ExecResult) exchange.getIn().getBody();
        InputStream is = body.getStdout();

        if (is == null) {
            exchange.getIn().setBody(body.getStderr());

            String line = null;
            BufferedReader br = new BufferedReader(new InputStreamReader(body.getStderr()));

            while ((line = br.readLine()) != null)
                logger.error(line);

            throw new Exception();
        }

        exchange.getIn().setBody(body.getStdout());
    }
}
