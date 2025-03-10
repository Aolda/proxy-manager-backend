package com.aolda.itda.template;

import org.springframework.stereotype.Component;

@Component
public class OptionTemplate {

    public String getSSL(Long certificateId) {
        return "\nconf.d/include/letsencrypt-acme-challenge.conf;\n" +
                "include conf.d/include/ssl-ciphers.conf;\n" +
                "ssl_certificate /etc/letsencrypt/live/npm-" + certificateId + "/fullchain.pem;\n" +
                "ssl_certificate_key /etc/letsencrypt/live/npm-" + certificateId + "/privkey.pem;\n";
    }

    public String getAssetCaching() {
        return "include conf.d/include/assets.conf;\n";
    }

    public String getBlockExploits() {
        return "include conf.d/include/block-exploits.conf;\n";
    }

    public String getForceSSL() {
        return "include conf.d/include/force-ssl.conf;\n";
    }
}
