package co.empresa.ticket_service.repository;

import co.empresa.ticket_service.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {

    Optional<Ticket> findByQrToken(String qrToken);

    Optional<Ticket> findByOrderId(String orderId);

    boolean existsByOrderId(String orderId);
}
