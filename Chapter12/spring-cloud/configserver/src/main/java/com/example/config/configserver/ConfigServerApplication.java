package com.example.config.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 *  The config server supports the storing of configuration files in a number of different backends, for example:
 *  • Git repository
 *  • Local filesystem
 *  • HashiCorp Vault
 *  • JDBC database
 *
 *  By default, a client connects first to the config server to retrieve its configuration. Based on the
 *  configuration, it connects to the discovery server, Netflix Eureka in our case, to register itself. It is also
 * possible to do this the other way around, that is, the client first connects to the discovery server to find a
 * config server instance and then connects to the config server to get its configuration. There are
 * pros and cons to both approaches.
 * In this chapter, the clients will first connect to the config server. With this approach, it will be possible
 * to store the configuration of the discovery server in the config server.
 *
 * When the configuration information is asked for by a microservice, or anyone using the API of the
 * config server, it will be protected against eavesdropping by the edge server since it already uses HTTPS.
 * To ensure that the API user is a known client, we will use HTTP basic authentication. We can set up
 * HTTP basic authentication by using Spring Security in the config server and specifying the environment
 * variables SPRING_SECURITY_USER_NAME and SPRING_SECURITY_USER_PASSWORD with the permitted credentials.
 *
 * Config server encrypts the configurations when it stores on git or any other way. For example:
 * Aap Git repo mein aise store karte ho:
 * spring.datasource.password={cipher}e1df09a0a9b3c...
 * Jab config server chal raha hota hai, wo automatically decrypt karke microservice ko plain password de
 * deta hai — lekin Git repo mein password encrypted hi hota hai.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigServerApplication.class, args);
	}

}
