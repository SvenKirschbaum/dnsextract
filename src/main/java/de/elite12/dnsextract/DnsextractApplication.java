package de.elite12.dnsextract;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;

@SpringBootApplication
@EnableIntegration
public class DnsextractApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(DnsextractApplication.class, args);
    }


    @Override
    public void run(String... args) throws Exception {
        while(true);
    }
}
