package com.redhat.fuse.commands.processors;

import com.redhat.fuse.commands.Response;
import com.redhat.fuse.commands.exceptions.MissingBundleStateException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BundleStateProcessor implements Processor {

    private Logger logger = LoggerFactory.getLogger(BundleStateProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        String result = (String) exchange.getIn().getBody();

        Pattern r = Pattern.compile("(\\[[a-zA-Z]+\\s+\\]+)");
        Matcher m = r.matcher(result);

        Response response = new Response();

        if (m.find()) {
            response.setData(parseBundleState(m.group(1)));
        } else {
            String error = "Could not find bundle state in: " + result;
            logger.error(error);
            throw new MissingBundleStateException(error);
        }

        response.setStatus(Response.SUCCESS);
        exchange.getIn().setBody(response);
    }

    private String parseBundleState(String bundleStateRaw) {
        return bundleStateRaw.substring(1, bundleStateRaw.length() - 1).trim();
    }
}
