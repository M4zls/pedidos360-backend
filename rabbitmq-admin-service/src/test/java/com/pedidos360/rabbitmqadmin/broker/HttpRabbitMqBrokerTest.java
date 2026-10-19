package com.pedidos360.rabbitmqadmin.broker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.pedidos360.rabbitmqadmin.config.RabbitMqManagementProperties;

/** Verifica las URLs exactas que se mandan a la Management API (vhost "/" -> "%2F"). */
class HttpRabbitMqBrokerTest {

    private static final String API = "http://rabbit:15672/api";

    private MockRestServiceServer server;
    private HttpRabbitMqBroker broker;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(API);
        server = MockRestServiceServer.bindTo(builder).build();
        broker = new HttpRabbitMqBroker(builder.build(),
                new RabbitMqManagementProperties("rabbit", 15672, "guest", "guest", "/"));
    }

    @Test
    void creaLaColaEnElVhostPorDefecto() {
        server.expect(requestTo(API + "/queues/%2F/pagos.pendientes"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("""
                        {"durable":true,"auto_delete":false,"arguments":{"x-dead-letter-exchange":"pagos.dlx"}}
                        """))
                .andRespond(withStatus(HttpStatus.CREATED));

        broker.createQueue("pagos.pendientes", true, false, Map.of("x-dead-letter-exchange", "pagos.dlx"));

        server.verify();
    }

    @Test
    void listaLosExchangesSinElExchangePorDefecto() {
        server.expect(requestTo(API + "/exchanges/%2F"))
                .andRespond(withSuccess("""
                        [{"name":"","type":"direct","durable":true,"auto_delete":false},
                         {"name":"orders.direct","type":"direct","durable":true,"auto_delete":false}]
                        """, MediaType.APPLICATION_JSON));

        List<ExchangeInfo> exchanges = broker.listExchanges();

        assertThat(exchanges).extracting(ExchangeInfo::name).containsExactly("orders.direct");
    }

    @Test
    void borraElBindingBuscandoSuPropertiesKey() {
        server.expect(requestTo(API + "/bindings/%2F/e/orders.direct/q/inventory.stock-reserve"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"routing_key":"order.created","properties_key":"order.created"}]
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(API + "/bindings/%2F/e/orders.direct/q/inventory.stock-reserve/order.created"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        broker.deleteBinding("orders.direct", BindingDestinationType.QUEUE, "inventory.stock-reserve", "order.created");

        server.verify();
    }

    @Test
    void unErrorDelBrokerConservaSuStatus() {
        server.expect(requestTo(API + "/queues/%2F/no.existe"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("{\"error\":\"Object Not Found\"}"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> broker.deleteQueue("no.existe"))
                .isInstanceOf(RabbitMqManagementException.class)
                .satisfies(ex -> assertThat(((RabbitMqManagementException) ex).status().value()).isEqualTo(404));
    }
}
