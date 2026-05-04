package com.team22.eventticketing.ticket.repository;

import com.team22.eventticketing.ticket.model.TicketScanEvent;
import com.team22.eventticketing.ticket.model.TicketScanEventKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketScanEventRepository
        extends CassandraRepository<TicketScanEvent, TicketScanEventKey> {
}