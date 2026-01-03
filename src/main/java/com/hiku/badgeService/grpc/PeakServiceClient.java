package com.hiku.badgeService.grpc;

import com.hiku.grpc.peak.PeakRequest;
import com.hiku.grpc.peak.PeakResponse;
import com.hiku.grpc.peak.PeakServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ApplicationScoped 
public class PeakServiceClient {

    private static final Logger logger = Logger.getLogger(PeakServiceClient.class.getName());
    private static final String GRPC_HOST = System.getenv().getOrDefault("PEAKS_GRPC_HOST", "peaks-hikes-service");
    private static final int GRPC_PORT = Integer.parseInt(System.getenv().getOrDefault("PEAKS_GRPC_PORT", "9090"));

    private final ManagedChannel channel;
    private final PeakServiceGrpc.PeakServiceBlockingStub blockingStub;

    public PeakServiceClient() {
        this.channel = ManagedChannelBuilder.forAddress(GRPC_HOST, GRPC_PORT)
                .usePlaintext()
                .build();
        this.blockingStub = PeakServiceGrpc.newBlockingStub(channel);
        logger.info("gRPC client connected to " + GRPC_HOST + ":" + GRPC_PORT);
    }

    public PeakResponse getPeakById(int peakId) {
        try {
            PeakRequest request = PeakRequest.newBuilder()
                    .setPeakId(peakId)
                    .build();
            return blockingStub.getPeakById(request);
        } catch (Exception e) {
            logger.warning("Failed to fetch peak " + peakId + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            if (e.getCause() != null) { 
                logger.warning("Root cause: " + e.getCause().getMessage());
            }
            e.printStackTrace();
            return PeakResponse.newBuilder()
                    .setId(peakId)
                    .setFound(false)
                    .build();
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warning("gRPC channel shutdown interrupted");
        }
    }
}
