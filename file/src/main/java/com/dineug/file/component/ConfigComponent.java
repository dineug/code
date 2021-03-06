package com.dineug.file.component;

import com.dineug.file.util.ENV;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * config data
 */
@Component
@Getter
public class ConfigComponent {

    @Value("${dineug.env}")
    private String env;

    @Value("${dineug.azure.blob}")
    private String azureBlob;

    @Value("${dineug.azure.key}")
    private String azureKey;

    public ENV getEnv() {
        if (env.equals("prod")) {
            return ENV.prod;
        } else if (env.equals("dev")) {
            return ENV.dev;
        } else if (env.equals("local")) {
            return ENV.local;
        } else {
            return ENV.test;
        }
    }

}
