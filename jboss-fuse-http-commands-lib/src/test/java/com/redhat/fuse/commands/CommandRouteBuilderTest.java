package com.redhat.fuse.commands;

import com.redhat.fuse.commands.routes.CommandRouteBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.exec.ExecBinding;
import org.apache.camel.component.exec.ExecResult;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Properties;

public class CommandRouteBuilderTest extends CamelTestSupport {

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties properties = new Properties();

        properties.put("host", "localhost");
        properties.put("port", "5556");
        properties.put("sourcePath", "src/test/resources");
        properties.put("fuse.workingDir", "");
        properties.put("basePath", "http");

        return properties;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new CommandRouteBuilder();
    }

    @Test
    public void testResolvePortWithContainerNameInCommandFile() throws Exception {

        context.getRouteDefinition("dispatchCommandRoute").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:in");

                interceptSendToEndpoint("direct:execute")
                    .skipSendToOriginalEndpoint()
                    .to("mock:end");
            }
        });

        context.getRouteDefinition("getPortFromContainerInfoRoute").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("execPortCommand")
                    .replace()
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/container_info_response")));

                            exchange.getIn().setHeader(ExecBinding.EXEC_EXIT_VALUE, 0);
                            exchange.getIn().setBody(result);
                        }
                    }).to("mock:exec");
            }
        });

        MockEndpoint mockEndpoint = getMockEndpoint("mock:end");

        mockEndpoint.setExpectedCount(1);
        mockEndpoint.expectedHeaderReceived("port", "8110");

        MockEndpoint mockExecEndpoint = getMockEndpoint("mock:exec");

        mockExecEndpoint.expectedHeaderReceived("container", "test");

        template.sendBodyAndHeader("direct:in", null, "command", "command_list_with_container");

        mockEndpoint.assertIsSatisfied();
        mockExecEndpoint.assertIsSatisfied();
    }

    @Test
    public void testExecuteCommandWithPortInCommandFile() throws Exception {
        context.getRouteDefinition("dispatchCommandRoute").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:in");

                interceptSendToEndpoint("direct:execute")
                    .skipSendToOriginalEndpoint()
                    .to("mock:end");
            }
        });

        MockEndpoint mockEndpoint = getMockEndpoint("mock:end");

        mockEndpoint.setExpectedCount(1);
        mockEndpoint.expectedHeaderReceived("port", "8102");

        template.sendBodyAndHeader("direct:in", null, "command", "command_list_with_port");

        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void testGetInputStreamObject() throws Exception {

        context.getRouteDefinition("executeCommandRoute").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("execCommand")
                    .replace()
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/command_success_response")));

                            exchange.getIn().setBody(result);
                        }
                    }).to("mock:exec");

                weaveById("processResponseCommand").after().to("mock:end");
            }
        });

        MockEndpoint mockEndpoint = getMockEndpoint("mock:end");
        mockEndpoint.message(0).body().isInstanceOf(String.class);

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("port", "8110");
        map.put("command", "list");

        template.sendBodyAndHeaders("direct:execute", null, map);

        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void testGetBundleState() throws Exception {

        context.getRouteDefinition("dispatchBundleStateRoute").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:dispatchBundleStateRoute");

                weaveById("marshalJsonResponse")
                    .after()
                    .to("mock:end");
            }
        });

        context.getRouteDefinition("getPortFromContainerInfoRoute").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("execPortCommand")
                    .replace()
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/container_info_response")));

                            exchange.getIn().setHeader(ExecBinding.EXEC_EXIT_VALUE, 0);
                            exchange.getIn().setBody(result);
                        }
                    });
            }
        });

        context.getRouteDefinition("executeCommandRoute").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("execCommand").replace().process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String response = "[ 211] [Active     ] [Created     ] [       ] [   80] jboss-fuse-http-commands (1.0.0.SNAPSHOT)\n";
                        exchange.getIn().setBody(response);
                    }
                });
            }
        });

        MockEndpoint mockEndpoint = getMockEndpoint("mock:end");
        mockEndpoint.expectedBodiesReceived("{\"status\":0,\"data\":\"Active\"}");

        mockEndpoint.message(0).header("name").isEqualTo("jboss-fuse-http-commands");
        mockEndpoint.message(0).header("container").isEqualTo("commands");

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("container", "commands");
        map.put("name", "jboss-fuse-http-commands");

        template.sendBodyAndHeaders("direct:dispatchBundleStateRoute", null, map);

        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void testGetBundleStateWithMissingBundleStateException() throws Exception {

        context.getRouteDefinition("dispatchBundleStateRoute").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:dispatchBundleStateRoute");

                weaveById("marshalJsonErrorResponse")
                    .after()
                    .to("mock:end");
            }
        });

        context.getRouteDefinition("getPortFromContainerInfoRoute").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("execPortCommand")
                    .replace()
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/container_info_response")));

                            exchange.getIn().setHeader(ExecBinding.EXEC_EXIT_VALUE, 0);
                            exchange.getIn().setBody(result);
                        }
                    });
            }
        });

        context.getRouteDefinition("executeCommandRoute").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("execCommand").replace().process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String response = "BAD RESPONSE";

                        exchange.getIn().setBody(response);
                    }
                });
            }
        });

        MockEndpoint mockEndpoint = getMockEndpoint("mock:end");
        mockEndpoint.expectedBodiesReceived("{\"status\":-1,\"data\":\"Could not find bundle state in: BAD RESPONSE\"}");

        mockEndpoint.message(0).header("name").isEqualTo("jboss-fuse-http-commands");
        mockEndpoint.message(0).header("container").isEqualTo("commands");

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("container", "commands");
        map.put("name", "jboss-fuse-http-commands");

        template.sendBodyAndHeaders("direct:dispatchBundleStateRoute", null, map);

        mockEndpoint.assertIsSatisfied();
    }


    @Test
    public void testGetBundleStateWithInvalidPortException() throws Exception {

        context.getRouteDefinition("dispatchBundleStateRoute").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:dispatchBundleStateRoute");

                weaveById("marshalJsonErrorResponse")
                    .after()
                    .to("mock:end");
            }
        });

        context.getRouteDefinition("getPortFromContainerInfoRoute").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("execPortCommand")
                    .replace()
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/container_info_bad_response")));

                            exchange.getIn().setHeader(ExecBinding.EXEC_EXIT_VALUE, 0);
                            exchange.getIn().setBody(result);
                        }
                    });
            }
        });

        context.getRouteDefinition("executeCommandRoute").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("execCommand").replace().process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String response = "[ 211] [Active     ] [Created     ] [       ] [   80] jboss-fuse-http-commands (1.0.0.SNAPSHOT)\n";

                        exchange.getIn().setBody(response);
                    }
                });
            }
        });

        MockEndpoint mockEndpoint = getMockEndpoint("mock:end");
        mockEndpoint.expectedBodiesReceived("{\"status\":-1,\"data\":\"Invalid port. The container may not exists.\"}");

        mockEndpoint.message(0).header("name").isEqualTo("jboss-fuse-http-commands");
        mockEndpoint.message(0).header("container").isEqualTo("commands");

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("container", "commands");
        map.put("name", "jboss-fuse-http-commands");

        template.sendBodyAndHeaders("direct:dispatchBundleStateRoute", null, map);

        mockEndpoint.assertIsSatisfied();
    }
}
