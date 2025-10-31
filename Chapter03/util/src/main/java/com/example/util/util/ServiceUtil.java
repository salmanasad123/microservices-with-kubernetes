package com.example.util.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The main purpose of ServiceUtil.java is to find out the hostname, IP address, and port
 * used by the microservice. The class exposes a method, getServiceAddress(), that can be used by
 * the microservices to find their hostname, IP address, and port.
 */

// @Component means this is a spring managed bean. Spring will create its object.
@Component
public class ServiceUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceUtil.class);

    private final String port;

    private String serviceAddress = null;

    // Agar class me do ya zyada constructors hain to Spring confuse ho sakta hai ke kaunsa use
    // kare bean banate waqt. Us case me tum @Autowired lagate ho to batate ho: “Ye wala constructor use karo injection ke liye.”
    // Yahaan @Autowired zaruri hai, warna Spring default constructor choose kar lega.
    // @Value property file (application.properties / application.yml) se ek value inject karta hai.
    @Autowired
    public ServiceUtil(@Value("${server.port}") String port) {
        this.port = port;
    }

    public String getServiceAddress() {
        if (serviceAddress == null) {
            serviceAddress = findMyHostname() + "/" + findMyIpAddress() + ":" + port;
        }
        return serviceAddress;
    }

    private String findMyHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown host name";
        }
    }

    private String findMyIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "unknown IP address";
        }
    }
}
