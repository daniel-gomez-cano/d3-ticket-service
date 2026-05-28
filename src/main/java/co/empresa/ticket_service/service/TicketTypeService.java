package co.empresa.ticket_service.service;

import co.empresa.ticket_service.dto.CreateTicketTypeRequest;
import co.empresa.ticket_service.dto.TicketTypeResponse;
import co.empresa.ticket_service.dto.UpdateTicketTypeRequest;
import co.empresa.ticket_service.model.TicketType;
import co.empresa.ticket_service.repository.TicketTypeRepository;
import co.empresa.ticket_service.config.EventServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketTypeService {
    private final EventServiceClient eventServiceClient;
    private final TicketTypeRepository repo;

    // SCRUM-31: Crear
    @Transactional
    public TicketTypeResponse create(CreateTicketTypeRequest req, String organizerId) {

         eventServiceClient.verifyEventOwnership(req.getEventId(), organizerId);
         
        TicketType tt = TicketType.builder()
                .eventId(req.getEventId())
                .name(req.getName())
                .price(req.getPrice())
                .totalCapacity(req.getTotalCapacity())
                .organizerId(organizerId)
                .active(true)
                .build();

        return toResponse(repo.save(tt));
    }

    // SCRUM-32: Listar — clientes solo ven activos
    public List<TicketTypeResponse> listActiveByEvent(String eventId) {
        return repo.findByEventIdAndActiveTrue(eventId)
                .stream().map(this::toResponse).toList();
    }

    // SCRUM-32: Listar — organizadores/admin ven todos
    public List<TicketTypeResponse> listAllByEvent(String eventId) {
        return repo.findByEventId(eventId)
                .stream().map(this::toResponse).toList();
    }

    // SCRUM-33: Editar
    @Transactional
    public TicketTypeResponse update(String id, UpdateTicketTypeRequest req, String organizerId) {
        TicketType tt = findAndVerifyOwnership(id, organizerId);

        if (req.getName() != null)          tt.setName(req.getName());
        if (req.getPrice() != null)         tt.setPrice(req.getPrice());
        if (req.getActive() != null)        tt.setActive(req.getActive());
        if (req.getTotalCapacity() != null) {
            int diff = req.getTotalCapacity() - tt.getTotalCapacity();
            tt.setTotalCapacity(req.getTotalCapacity());
            tt.setRemainingCapacity(Math.max(0, tt.getRemainingCapacity() + diff));
        }

        return toResponse(repo.save(tt));
    }

    // SCRUM-34: Eliminar
    @Transactional
    public void delete(String id, String organizerId) {
        repo.delete(findAndVerifyOwnership(id, organizerId));
    }

    /**
     * Usado por TicketService al generar boletas.
     * Decrementa remainingCapacity de forma atómica dentro de @Transactional.
     */
    @Transactional
    public TicketType reserveOne(String ticketTypeId) {
        TicketType tt = repo.findById(ticketTypeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tipo de boleta no encontrado: " + ticketTypeId));

        if (!tt.isActive())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este tipo de boleta no está activo");
        if (tt.getRemainingCapacity() <= 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No hay cupos disponibles");

        tt.setRemainingCapacity(tt.getRemainingCapacity() - 1);
        return repo.save(tt);
    }

    private TicketType findAndVerifyOwnership(String id, String organizerId) {
        TicketType tt = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tipo de boleta no encontrado"));
        if (!tt.getOrganizerId().equals(organizerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permiso para modificar este tipo de boleta");
        return tt;
    }

    private TicketTypeResponse toResponse(TicketType tt) {
        return TicketTypeResponse.builder()
                .id(tt.getId())
                .eventId(tt.getEventId())
                .name(tt.getName())
                .price(tt.getPrice())
                .totalCapacity(tt.getTotalCapacity())
                .remainingCapacity(tt.getRemainingCapacity())
                .active(tt.isActive())
                .organizerId(tt.getOrganizerId())
                .createdAt(tt.getCreatedAt())
                .updatedAt(tt.getUpdatedAt())
                .build();
    }
}
