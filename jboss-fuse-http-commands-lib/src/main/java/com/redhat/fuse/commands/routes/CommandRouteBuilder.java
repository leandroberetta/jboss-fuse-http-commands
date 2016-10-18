package com.redhat.fuse.commands.routes;

import com.redhat.fuse.commands.exceptions.ExecCommandException;
import com.redhat.fuse.commands.processors.*;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.exec.ExecBinding;
import org.apache.camel.model.dataformat.JsonLibrary;

public class CommandRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        FileCommandProcessor fileCommandProcessor = new FileCommandProcessor();
        GetPortProcessor getPortProcessor = new GetPortProcessor();
        BundleStateProcessor bundleStateProcessor = new BundleStateProcessor();

        onException()
            .handled(true)
            .process(new ErrorProcessor())
            .marshal().json(JsonLibrary.Jackson).id("marshalJsonErrorResponse");

        from("restlet:http://{{host}}:{{port}}/{{basePath}}/commands/container/{container}/bundle-state/{name}")
            .routeId("dispatchBundleStateRoute")
            .to("direct:getPortFromContainerInfo")
            .setHeader("command", simple("list -s |grep ${header.name}"))
            .to("direct:execute")
            .process(new Processor() {
                @Override
                public void process(Exchange exchange) throws Exception {
                    if (exchange.getIn().getHeader(ExecBinding.EXEC_EXIT_VALUE, Integer.class) == 1) // ERROR
                        throw new ExecCommandException("Error executing command.");
                }
            })
            .process(bundleStateProcessor)
            .marshal().json(JsonLibrary.Jackson).id("marshalJsonResponse");

        from("restlet:http://{{host}}:{{port}}/{{basePath}}/commands/{command}")
            .routeId("dispatchCommandRoute")
            .setHeader("sourcePath", simple("{{sourcePath}}"))
            .process(fileCommandProcessor)
            .choice()
                .when(header("target").isEqualTo("container")).to("direct:getPortFromContainerInfo").to("direct:execute")
                .when(header("target").isEqualTo("port")).to("direct:execute")
                .otherwise().stop();

        from("direct:getPortFromContainerInfo")
            .routeId("getPortFromContainerInfoRoute")
            .setHeader(ExecBinding.EXEC_COMMAND_ARGS, simple("-a 8101 container-info ${header.container}"))
            .to("exec:./client?workingDir={{fuse.workingDir}}").id("execPortCommand")
            .process(new Processor() {
                @Override
                public void process(Exchange exchange) throws Exception {
                    if (exchange.getIn().getHeader(ExecBinding.EXEC_EXIT_VALUE, Integer.class) == 1) // ERROR
                        throw new ExecCommandException("Error executing command.");
                }
            })
            .convertBodyTo(String.class)
            .process(getPortProcessor);

        from("direct:execute")
            .routeId("executeCommandRoute")
            .setHeader(ExecBinding.EXEC_COMMAND_ARGS, simple("-a ${header.port} \"${header.command}\""))
            .to("exec:./client?workingDir={{fuse.workingDir}}").id("execCommand")
            .convertBodyTo(String.class).id("processResponseCommand");
    }
}