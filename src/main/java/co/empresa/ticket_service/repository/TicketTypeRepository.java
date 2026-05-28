package co.empresa.ticket_service.repository;

import co.empresa.ticket_service.model.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, String> {

    /** Todos los tipos de boleta de un evento (para el endpoint público y organizador) */
    List<TicketType> findByEventId(String eventId);

    /** Solo los activos — para compradores */
    List<TicketType> findByEventIdAndActiveTrue(String eventId);

    /** Para verificar que el organizador es dueño antes de editar/eliminar */
    boolean existsByIdAndOrganizerId(String id, String organizerId);
}
