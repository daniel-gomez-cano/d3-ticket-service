package co.empresa.ticket_service.service;

import co.empresa.ticket_service.dto.CreateTicketRequest;
import co.empresa.ticket_service.dto.TicketResponse;
import co.empresa.ticket_service.dto.ValidationResult;
import co.empresa.ticket_service.model.Ticket;
import co.empresa.ticket_service.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService — Tests unitarios")
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepo;

    @Mock
    private QrService qrService;

    @InjectMocks
    private TicketService ticketService;

    private CreateTicketRequest validRequest;
    private Ticket activeTicket;

    @BeforeEach
    void setUp() {
        validRequest = new CreateTicketRequest(
                "type-001", "General", "event-001", "order-001", "buyer-001"
        );

        activeTicket = Ticket.builder()
                .id("ticket-001")
                .ticketTypeId("type-001")
                .ticketTypeName("General")
                .eventId("event-001")
                .orderId("order-001")
                .buyerId("buyer-001")
                .qrToken("qr-token-001")
                .qrImageBase64("base64data")
                .status(Ticket.TicketStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // generateTicket

    @Test
    @DisplayName("generateTicket: crea boleta nueva exitosamente")
    void generateTicket_createsNewTicket() {
        when(ticketRepo.findByOrderId("order-001")).thenReturn(Optional.empty());
        when(qrService.generateQrBase64(anyString())).thenReturn("base64data");
        when(ticketRepo.save(any(Ticket.class))).thenReturn(activeTicket);

        TicketResponse response = ticketService.generateTicket(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo("order-001");
        assertThat(response.getBuyerId()).isEqualTo("buyer-001");
        assertThat(response.getStatus()).isEqualTo(Ticket.TicketStatus.ACTIVE);
        assertThat(response.getQrImageBase64()).isNotBlank();
        verify(ticketRepo).save(any(Ticket.class));
        verify(qrService).generateQrBase64(anyString());
    }

    @Test
    @DisplayName("generateTicket: idempotente — retorna boleta existente sin crear duplicado")
    void generateTicket_idempotent_returnsExisting() {
        when(ticketRepo.findByOrderId("order-001")).thenReturn(Optional.of(activeTicket));

        TicketResponse response = ticketService.generateTicket(validRequest);

        assertThat(response.getId()).isEqualTo("ticket-001");
        verify(ticketRepo, never()).save(any());
        verify(qrService, never()).generateQrBase64(anyString());
    }

    @Test
    @DisplayName("generateTicket: DataIntegrityViolation por concurrencia — retorna ticket ganador")
    void generateTicket_handlesConcurrentDuplicate() {
        when(ticketRepo.findByOrderId("order-001"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(activeTicket));
        when(qrService.generateQrBase64(anyString())).thenReturn("base64data");
        when(ticketRepo.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        TicketResponse response = ticketService.generateTicket(validRequest);

        assertThat(response.getId()).isEqualTo("ticket-001");
    }

    // validateTicket

    @Test
    @DisplayName("validateTicket: boleta ACTIVE → valid=true, status cambia a USED")
    void validateTicket_activeTicket_returnsValidAndMarksUsed() {
        when(ticketRepo.findByQrToken("qr-token-001")).thenReturn(Optional.of(activeTicket));
        when(ticketRepo.save(any())).thenReturn(activeTicket);

        ValidationResult result = ticketService.validateTicket("qr-token-001");

        assertThat(result.isValid()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Acceso permitido");
        assertThat(result.getTicketId()).isEqualTo("ticket-001");
        assertThat(result.getEventId()).isEqualTo("event-001");
        assertThat(result.getBuyerId()).isEqualTo("buyer-001");
        assertThat(result.getValidatedAt()).isNotNull();
        assertThat(activeTicket.getStatus()).isEqualTo(Ticket.TicketStatus.USED);
        assertThat(activeTicket.getUsedAt()).isNotNull();
        verify(ticketRepo).save(activeTicket);
    }

    @Test
    @DisplayName("validateTicket: boleta USED → valid=false, no guarda de nuevo")
    void validateTicket_usedTicket_returnsInvalid() {
        activeTicket.setStatus(Ticket.TicketStatus.USED);
        activeTicket.setUsedAt(LocalDateTime.now().minusHours(1));
        when(ticketRepo.findByQrToken("qr-token-001")).thenReturn(Optional.of(activeTicket));

        ValidationResult result = ticketService.validateTicket("qr-token-001");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("ya utilizada");
        verify(ticketRepo, never()).save(any());
    }

    @Test
    @DisplayName("validateTicket: boleta CANCELLED → valid=false")
    void validateTicket_cancelledTicket_returnsInvalid() {
        activeTicket.setStatus(Ticket.TicketStatus.CANCELLED);
        when(ticketRepo.findByQrToken("qr-token-001")).thenReturn(Optional.of(activeTicket));

        ValidationResult result = ticketService.validateTicket("qr-token-001");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Boleta cancelada");
        verify(ticketRepo, never()).save(any());
    }

    @Test
    @DisplayName("validateTicket: QR inexistente → valid=false con mensaje claro")
    void validateTicket_unknownToken_returnsInvalid() {
        when(ticketRepo.findByQrToken("token-falso")).thenReturn(Optional.empty());

        ValidationResult result = ticketService.validateTicket("token-falso");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).isEqualTo("QR no reconocido");
        verify(ticketRepo, never()).save(any());
    }

    @Test
    @DisplayName("validateTicket: boleta USED incluye fecha de uso en el mensaje")
    void validateTicket_usedTicket_includesUsedAtInMessage() {
        LocalDateTime usedAt = LocalDateTime.of(2025, 6, 10, 20, 30);
        activeTicket.setStatus(Ticket.TicketStatus.USED);
        activeTicket.setUsedAt(usedAt);
        when(ticketRepo.findByQrToken("qr-token-001")).thenReturn(Optional.of(activeTicket));

        ValidationResult result = ticketService.validateTicket("qr-token-001");

        assertThat(result.getMessage()).contains("2025-06-10");
    }

    // getById

    @Test
    @DisplayName("getById: comprador consulta su propia boleta exitosamente")
    void getById_ownTicket_returnsResponse() {
        when(ticketRepo.findById("ticket-001")).thenReturn(Optional.of(activeTicket));

        TicketResponse response = ticketService.getById("ticket-001", "buyer-001");

        assertThat(response.getId()).isEqualTo("ticket-001");
        assertThat(response.getBuyerId()).isEqualTo("buyer-001");
        assertThat(response.getEventId()).isEqualTo("event-001");
    }

    @Test
    @DisplayName("getById: boleta no encontrada → 404")
    void getById_notFound_throws404() {
        when(ticketRepo.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getById("no-existe", "buyer-001"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex)
                        .getStatusCode().value()).isEqualTo(404));
    }

    @Test
    @DisplayName("getById: comprador intenta ver boleta ajena → 403")
    void getById_differentBuyer_throws403() {
        when(ticketRepo.findById("ticket-001")).thenReturn(Optional.of(activeTicket));

        assertThatThrownBy(() -> ticketService.getById("ticket-001", "otro-buyer"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex)
                        .getStatusCode().value()).isEqualTo(403));
    }
}
