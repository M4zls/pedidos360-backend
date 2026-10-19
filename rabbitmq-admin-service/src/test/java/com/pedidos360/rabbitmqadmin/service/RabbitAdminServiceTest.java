package com.pedidos360.rabbitmqadmin.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pedidos360.rabbitmqadmin.broker.BindingDestinationType;
import com.pedidos360.rabbitmqadmin.broker.ExchangeInfo;
import com.pedidos360.rabbitmqadmin.broker.QueueInfo;
import com.pedidos360.rabbitmqadmin.broker.RabbitMqBroker;

class RabbitAdminServiceTest {

    private RabbitMqBroker broker;
    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        broker = mock(RabbitMqBroker.class);
        service = new RabbitAdminService(broker);
        when(broker.listExchanges()).thenReturn(List.of(new ExchangeInfo("pagos.dlx", "direct", true, false)));
        when(broker.listQueues()).thenReturn(List.of(new QueueInfo("pagos.existente", true, false, 0, 0, Map.of())));
    }

    @Test
    void creaLaColaConSusArgumentosDeDeadLetter() {
        service.createQueue("pagos.pendientes", true, false, "pagos.dlx", "pagos.dlq");

        verify(broker).createQueue("pagos.pendientes", true, false, Map.of(
                "x-dead-letter-exchange", "pagos.dlx",
                "x-dead-letter-routing-key", "pagos.dlq"));
    }

    @Test
    void creaLaColaSinArgumentosSiNoHayDeadLetter() {
        service.createQueue("pagos.pendientes", true, false, null, null);

        verify(broker).createQueue("pagos.pendientes", true, false, Map.of());
    }

    @Test
    void noPisaUnaColaExistente() {
        assertThatThrownBy(() -> service.createQueue("pagos.existente", true, false, null, null))
                .isInstanceOf(TopologyConflictException.class);
        verify(broker, never()).createQueue(anyString(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void rechazaUnaDeadLetterExchangeQueNoExiste() {
        assertThatThrownBy(() -> service.createQueue("pagos.pendientes", true, false, "no.existe", null))
                .isInstanceOf(InvalidTopologyException.class)
                .hasMessageContaining("no.existe");
        verify(broker, never()).createQueue(anyString(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void noPisaUnExchangeExistente() {
        assertThatThrownBy(() -> service.createExchange("pagos.dlx", "topic", true, false))
                .isInstanceOf(TopologyConflictException.class);
        verify(broker, never()).createExchange(anyString(), anyString(), anyBoolean(), anyBoolean());
    }

    @Test
    void bindingConOrigenODestinoInexistenteSeRechaza() {
        assertThatThrownBy(() -> service.createBinding("no.existe", BindingDestinationType.QUEUE, "pagos.existente", "k"))
                .isInstanceOf(InvalidTopologyException.class);
        assertThatThrownBy(() -> service.createBinding("pagos.dlx", BindingDestinationType.QUEUE, "no.existe", "k"))
                .isInstanceOf(InvalidTopologyException.class);
        verify(broker, never()).createBinding(anyString(), any(), anyString(), anyString());
    }

    @Test
    void bindingValidoLlegaAlBroker() {
        service.createBinding("pagos.dlx", BindingDestinationType.QUEUE, "pagos.existente", "pagos.dlq");

        verify(broker).createBinding("pagos.dlx", BindingDestinationType.QUEUE, "pagos.existente", "pagos.dlq");
    }
}
