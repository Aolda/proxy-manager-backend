package com.aolda.itda.template;

import com.aolda.itda.dto.routing.RoutingDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoutingTemplate {

    private final OptionTemplate optionTemplate;

    public String getRouting(RoutingDTO dto, String certificateDomain) {
        return "server { \n"
                + "set $forward_scheme http;\n"
                + "set $server \"" + dto.getIp() +"\";\n"
                + "set $port "+ dto.getPort() +";\n"
                + "\n"
                + "listen 80;\n"
                + "listen [::]:80;\n"
                + (dto.getCertificateId() == -1 ? "" :
                "listen 443 ssl;\nlisten [::]:443 ssl;\n")
                + "server_name " + dto.getDomain() + ";\n"
                + (dto.getCertificateId() == -1 ? "" :
                optionTemplate.getSSL(certificateDomain))
                + (!dto.getCaching() ? "" :
                optionTemplate.getAssetCaching()
        )
                + "proxy_set_header Upgrade $http_upgrade;\n"
                + "proxy_set_header Connection $http_connection;\n"
                + "proxy_http_version 1.1;\n"
                + "\n"
                + optionTemplate.getIncreasingProxyBufferSize()
                + "\n"
                + "access_log /data/logs/proxy-host-" + dto.getId() + "_access.log proxy;\n"
                + "error_log /data/logs/proxy-host-" + dto.getId() + "_error.log warn;\n"
                + "location / { \n"
                + "proxy_set_header Upgrade $http_upgrade;\n"
                + "proxy_set_header Connection $http_connection;\n"
                + "proxy_http_version 1.1;\n"
                + "include conf.d/include/proxy.conf;\n"
                + "}\n"
                + "}\n";
    }

}
