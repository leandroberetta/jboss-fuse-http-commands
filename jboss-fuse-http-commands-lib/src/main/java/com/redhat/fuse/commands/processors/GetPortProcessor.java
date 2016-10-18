package com.redhat.fuse.commands.processors;

import com.redhat.fuse.commands.exceptions.InvalidPortException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetPortProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        String result = (String) exchange.getIn().getBody();
        String port = null;

        Pattern r = Pattern.compile(".*?SSH Url.*?:(\\d{3,5}).*?");
        Matcher m = r.matcher(result);

        if (m.find()) {
            port = m.group(1);
        }

        if (port == null)
            throw new InvalidPortException("The container does not exists");

        exchange.getIn().setHeader("port", port);
    }
}
