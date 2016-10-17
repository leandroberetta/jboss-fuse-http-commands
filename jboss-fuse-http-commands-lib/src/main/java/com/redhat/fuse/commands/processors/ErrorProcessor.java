package com.redhat.fuse.commands.processors;

import com.redhat.fuse.commands.Response;
import com.redhat.fuse.commands.Response;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorProcessor implements Processor {

    private Logger logger = LoggerFactory.getLogger(ErrorProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        Throwable caused = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);

        logger.error(caused.getLocalizedMessage());

        Response response  = new Response();
        response.setStatus(Response.ERROR);
        response.setData(caused.getLocalizedMessage());

        exchange.getIn().setBody(response);
    }
}
