package de.elite12.dnsextract;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.ip.dsl.Udp;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.integration.ip.udp.UnicastSendingMessageHandler;
import org.springframework.messaging.MessageChannel;

@Configuration
public class AppConfig {

    @Bean
    public IntegrationFlow inflow() {
        return IntegrationFlows.from(Udp.inboundAdapter(53).id("inbound").lookupHost(false))
                .channel("udpIn")
                .get();
    }

    @Bean
    public IntegrationFlow outflow() {
        return IntegrationFlows.from("udpOut")
                .handle(
                    Udp.outboundAdapter("headers['ip_packetAddress']")
                        .socketExpression("@inbound.socket")
                        .get()
                )
                .get();
    }
}
