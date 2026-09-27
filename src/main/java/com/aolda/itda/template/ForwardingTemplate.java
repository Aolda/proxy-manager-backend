package com.aolda.itda.template;

import org.springframework.stereotype.Component;

@Component
public class ForwardingTemplate {

    public String getPortForwardingWithTCP(String serverPort, String instanceIp, String instancePort, String name, boolean proxyProtocol) {
        return  "# " + name + "\n" +
                "server { \n" +
                " listen " + serverPort + "; \n" +
                " listen [::]:" + serverPort + "; \n" +
                (proxyProtocol ? " proxy_protocol on;\n" : "") +
                " proxy_pass " + instanceIp + ":" + instancePort + ";\n" +
                "} \n";
    }
}
