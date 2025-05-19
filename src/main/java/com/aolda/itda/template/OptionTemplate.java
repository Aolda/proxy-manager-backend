package com.aolda.itda.template;

import org.springframework.stereotype.Component;

@Component
public class OptionTemplate {

    public String getSSL(String certificateDomain) {
        return "\ninclude conf.d/include/letsencrypt-acme-challenge.conf;\n" +
                "include conf.d/include/ssl-ciphers.conf;\n" +
                "ssl_certificate /data/lego/certificates/" + certificateDomain + ".crt;\n" +
                "ssl_certificate_key /data/lego/certificates/" + certificateDomain + ".key;\n";
    }

    public String getAssetCaching() {
        return "\ninclude conf.d/include/assets.conf;\n";
    }

    public String getBlockExploits() {
        return "\ninclude conf.d/include/block-exploits.conf;\n";
    }

    public String getForceSSL() {
        return "\ninclude conf.d/include/force-ssl.conf;\n";
    }
}
