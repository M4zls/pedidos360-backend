package com.pedidos360.rabbitmqadmin.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.pedidos360.rabbitmqadmin.broker.BindingDestinationType;
import com.pedidos360.rabbitmqadmin.broker.QueueInfo;
import com.pedidos360.rabbitmqadmin.broker.RabbitMqManagementException;
import com.pedidos360.rabbitmqadmin.service.InvalidTopologyException;
import com.pedidos360.rabbitmqadmin.service.RabbitAdminService;
import com.pedidos360.rabbitmqadmin.service.TopologyConflictException;

class RabbitMqAdminControllerTest {

    private static final String BASE = "/api/admin/rabbitmq";

    private RabbitAdminService rabbitAdmin;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        rabbitAdmin = mock(RabbitAdminService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new RabbitMqAdminController(rabbitAdmin))
                .setControllerAdvice(new RabbitMqAdminExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ---- crear ----

    @Test
    void postQueueCreaLaColaYDevuelve201ConLocation() throws Exception {
        mvc.perform(post(BASE + "/queues").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"pagos.pendientes","durable":true,"deadLetterExchange":"pagos.dlx","deadLetterRoutingKey":"pagos.dlq"}
                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(BASE + "/queues/pagos.pendientes")));
        verify(rabbitAdmin).createQueue("pagos.pendientes", true, false, "pagos.dlx", "pagos.dlq");
    }

    @Test
    void postExchangeCreaElExchangeYDevuelve201() throws Exception {
        mvc.perform(post(BASE + "/exchanges").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"pagos.topic","type":"TOPIC","durable":true}
                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(BASE + "/exchanges/pagos.topic")));
        verify(rabbitAdmin).createExchange("pagos.topic", "topic", true, false);
    }

    @Test
    void postBindingDevuelve201() throws Exception {
        mvc.perform(post(BASE + "/bindings").contentType(MediaType.APPLICATION_JSON).content("""
                {"source":"pagos.topic","destinationType":"QUEUE","destination":"pagos.pendientes","routingKey":"pago.#"}
                """))
                .andExpect(status().isCreated());
        verify(rabbitAdmin).createBinding("pagos.topic", BindingDestinationType.QUEUE, "pagos.pendientes", "pago.#");
    }

    // ---- validacion de la entrada ----

    @Test
    void rechazaColaConNombreVacio() throws Exception {
        mvc.perform(post(BASE + "/queues").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"   ","durable":true}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("name"))));
        verifyNoInteractions(rabbitAdmin);
    }

    @Test
    void rechazaColaSinNombre() throws Exception {
        mvc.perform(post(BASE + "/queues").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(rabbitAdmin);
    }

    @Test
    void rechazaNombreConCaracteresInvalidos() throws Exception {
        mvc.perform(post(BASE + "/queues").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"cola con espacios/y barras"}
                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(rabbitAdmin);
    }

    @Test
    void rechazaNombreReservadoAmq() throws Exception {
        mvc.perform(post(BASE + "/exchanges").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"amq.mio","type":"DIRECT"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("amq."))));
        verifyNoInteractions(rabbitAdmin);
    }

    @Test
    void rechazaNombreDeMasDe255Caracteres() throws Exception {
        String name = "a".repeat(256);
        mvc.perform(post(BASE + "/queues").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"%s"}
                """.formatted(name)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(rabbitAdmin);
    }

    @Test
    void rechazaDeadLetterRoutingKeySinExchange() throws Exception {
        mvc.perform(post(BASE + "/queues").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"pagos.pendientes","deadLetterRoutingKey":"pagos.dlq"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("deadLetterExchange"))));
        verifyNoInteractions(rabbitAdmin);
    }

    @Test
    void rechazaTipoDeExchangeDesconocido() throws Exception {
        mvc.perform(post(BASE + "/exchanges").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"pagos.x","type":"BROADCAST"}
                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(rabbitAdmin);
    }

    @Test
    void rechazaExchangeSinTipo() throws Exception {
        mvc.perform(post(BASE + "/exchanges").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"pagos.x"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("type"))));
        verifyNoInteractions(rabbitAdmin);
    }

    @Test
    void rechazaBindingSinOrigen() throws Exception {
        mvc.perform(post(BASE + "/bindings").contentType(MediaType.APPLICATION_JSON).content("""
                {"destinationType":"QUEUE","destination":"pagos.pendientes"}
                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(rabbitAdmin);
    }

    @Test
    void deleteQueueRechazaNombreReservado() throws Exception {
        mvc.perform(delete(BASE + "/queues/amq.gen-123"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(rabbitAdmin);
    }

    @Test
    void deleteBindingPorQueryParams() throws Exception {
        mvc.perform(delete(BASE + "/bindings")
                .param("source", "pagos.topic")
                .param("destinationType", "QUEUE")
                .param("destination", "pagos.pendientes")
                .param("routingKey", "pago.#"))
                .andExpect(status().isNoContent());
        verify(rabbitAdmin).deleteBinding("pagos.topic", BindingDestinationType.QUEUE, "pagos.pendientes", "pago.#");
    }

    @Test
    void deleteBindingRechazaParametrosFaltantesOInvalidos() throws Exception {
        mvc.perform(delete(BASE + "/bindings").param("source", "pagos.topic").param("destinationType", "QUEUE"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(BASE + "/bindings")
                .param("source", "pagos.topic")
                .param("destinationType", "COLA")
                .param("destination", "pagos.pendientes"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(rabbitAdmin);
    }

    // ---- consultar y borrar ----

    @Test
    void getQueuesListaLasColas() throws Exception {
        when(rabbitAdmin.listQueues()).thenReturn(List.of(
                new QueueInfo("orders.notifications", true, false, 3, 1, Map.of())));
        mvc.perform(get(BASE + "/queues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("orders.notifications"))
                .andExpect(jsonPath("$[0].messages").value(3));
    }

    @Test
    void deleteQueueDevuelve204() throws Exception {
        mvc.perform(delete(BASE + "/queues/pagos.pendientes"))
                .andExpect(status().isNoContent());
        verify(rabbitAdmin).deleteQueue("pagos.pendientes");
    }

    // ---- errores del servicio y del broker ----

    @Test
    void colaExistenteDevuelve409() throws Exception {
        doThrow(new TopologyConflictException("La cola 'x' ya existe"))
                .when(rabbitAdmin).createQueue(anyString(), anyBoolean(), anyBoolean(), any(), any());
        mvc.perform(post(BASE + "/queues").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void dlxInexistenteDevuelve400() throws Exception {
        doThrow(new InvalidTopologyException("La dead-letter-exchange 'y' no existe"))
                .when(rabbitAdmin).createQueue(anyString(), anyBoolean(), anyBoolean(), any(), any());
        mvc.perform(post(BASE + "/queues").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"x","deadLetterExchange":"y"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La dead-letter-exchange 'y' no existe"));
    }

    @Test
    void colaInexistenteEnElBrokerDevuelve404() throws Exception {
        doThrow(new RabbitMqManagementException(HttpStatusCode.valueOf(404), "Object Not Found"))
                .when(rabbitAdmin).deleteQueue("no.existe");
        mvc.perform(delete(BASE + "/queues/no.existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    void credencialesDelBrokerInvalidasDevuelve502() throws Exception {
        doThrow(new RabbitMqManagementException(HttpStatusCode.valueOf(401), "Not authorised"))
                .when(rabbitAdmin).listQueues();
        mvc.perform(get(BASE + "/queues"))
                .andExpect(status().isBadGateway());
    }
}
