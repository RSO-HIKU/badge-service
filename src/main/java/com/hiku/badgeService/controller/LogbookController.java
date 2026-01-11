package com.hiku.badgeService.controller;

import com.hiku.badgeService.db.models.LogbookEntry;
import com.hiku.badgeService.service.LogbookService;
import com.hiku.badgeService.grpc.PeakServiceClient;
import com.hiku.grpc.peak.PeakResponse;
import javax.annotation.security.RolesAllowed;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Path("/logbook")
@RolesAllowed("user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LogbookController {

    private static final DateTimeFormatter LOGBOOK_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    @Inject
    LogbookService logbookService;

    @Inject
    PeakServiceClient peakServiceClient;

    public static class LogbookRequest {
        public String userId;
        public Integer peakId;
        public String addedAt;
        public String notes;
    }

    public static class LogbookEntryDto {
        public Long id;
        public String userId;
        public Integer peakId;
        public String peakName;
        public String territory;
        public Double elevationM;
        public String addedAt;
        public String notes;
    }

    @GET
    public Response list(@QueryParam("userId") String userId) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"userId is required\"}")
                    .build();   
        }

        List<LogbookEntryDto> body = logbookService.listForUser(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return Response.ok(body).build();
    }

    @POST
    public Response add(LogbookRequest req) {
        if (req == null || req.userId == null || req.userId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"userId is required\"}")
                    .build();
        }
        if (req.peakId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"peakId is required\"}")
                    .build();
        }

        Instant addedAt = null;
        if (req.addedAt != null && !req.addedAt.isBlank()) {
            try {
                addedAt = Instant.parse(req.addedAt);
            } catch (Exception e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"addedAt must be ISO-8601 timestamp\"}")
                        .build();
            }
        }

        LogbookEntry entry = logbookService.addEntry(req.userId, req.peakId, addedAt, req.notes);
        return Response.status(Response.Status.CREATED).entity(toDto(entry)).build();
    }

    private LogbookEntryDto toDto(LogbookEntry e) {
        LogbookEntryDto dto = new LogbookEntryDto();
        dto.id = e.getId();
        dto.userId = e.getUserId();
        dto.peakId = e.getPeakId();
        dto.addedAt = e.getAddedAt() != null ? LOGBOOK_FORMATTER.format(e.getAddedAt()) : null;
        dto.notes = e.getNotes();
        
        // Fetch peak details via gRPC
        if (e.getPeakId() != null) {
            try {
                PeakResponse peakResponse = peakServiceClient.getPeakById(e.getPeakId());
                if (peakResponse.getFound()) {
                    dto.peakName = peakResponse.getName();
                    dto.territory = peakResponse.getTerritory();
                    dto.elevationM = peakResponse.getElevationM();
                } else {
                    dto.peakName = String.valueOf(e.getPeakId());
                }
            } catch (Exception ex) {
                // Fallback to peak ID if gRPC fails
                dto.peakName = String.valueOf(e.getPeakId());
            }
        }
        
        return dto;
    }
}
