package com.redhat.fuse.commands.processors;

import com.redhat.fuse.commands.Response;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BundleStateProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        InputStream body = (InputStream) exchange.getIn().getBody();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(body));
        String name = (String) exchange.getIn().getHeader("name");

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.contains(name))
                break;
        }

        Pattern r = Pattern.compile("([a-zA-Z0-9{}$._-]+)");
        Matcher m = r.matcher(line);
        Response response = new Response();

        // Skip first match
        m.find();
        if (m.find()) {
            response.setData(m.group(1));
        } else {
            response.setData("");
        }

        response.setStatus(Response.SUCCESS);

        exchange.getIn().setBody(response);
    }
}