package com.aolda.itda.template;

import org.springframework.stereotype.Component;

@Component
public class ForwardingTemplate {

    public String getPortForwardingWithTCP(String instanceIp, String serverPort) {
        return "\nlisten " + serverPort + "; \n" +
                "listen [::]:" + serverPort + "; \n" +
                "proxy_pass " + instanceIp + ";\n";
    }
}
