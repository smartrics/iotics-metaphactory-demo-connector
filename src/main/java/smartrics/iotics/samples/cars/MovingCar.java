package smartrics.iotics.samples.cars;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public class MovingCar {
    private static final Logger LOGGER = LoggerFactory.getLogger(MovingCar.class);

    private final double speedKmh;
    private final double maxDistance;
    private final Random rand;
    private double currentLat;
    private double currentLon;
    private double distanceCovered;
    private double direction; // in radians
    private boolean returning;
    private LocalDateTime lastUpdateTime;
    private LocationData locationData;
    private OperationalStatus operationalStatus;


    public MovingCar(double initialLat, double initialLon) {
        this.rand = new Random();
        this.currentLat = initialLat;
        this.currentLon = initialLon;
        this.maxDistance = rand.nextDouble(5, 20);
        this.distanceCovered = 0.0;
        this.returning = false;
        this.speedKmh = rand.nextDouble(45.0, 55.0);
        this.direction = rand.nextDouble() * 2 * Math.PI; // Random direction in radians
        this.lastUpdateTime = LocalDateTime.now();
    }

    public void update() {
        updatePosition();
        updateOperationalStatus();
        LOGGER.info("{} {}", currentOperationalStatus(), currentLocationData());
    }

    public OperationalStatus currentOperationalStatus() {
        return operationalStatus;
    }

    public LocationData currentLocationData() {
        return locationData;
    }

    private void updateOperationalStatus() {
        this.operationalStatus = new OperationalStatus(rand.nextBoolean());
    }

    private void updatePosition() {
        LocalDateTime currentTime = LocalDateTime.now();
        double hoursElapsed = ChronoUnit.SECONDS.between(lastUpdateTime, currentTime) / 3600.0;
        lastUpdateTime = currentTime;

        double distance = speedKmh * hoursElapsed;

        if (!returning) {
            distanceCovered += distance;
            if (distanceCovered >= maxDistance) {
                returning = true;
                direction += Math.PI; // Reverse direction
                distance = distanceCovered - maxDistance;
                distanceCovered = maxDistance;
            }
        } else {
            distanceCovered -= distance;
            if (distanceCovered <= 0) {
                returning = false;
                direction += Math.PI; // Reverse direction back to original
                distance = -distanceCovered;
                distanceCovered = 0;
            }
        }

        // Calculate new position
        double earthRadiusKm = 6371.0;
        double dLat = distance / earthRadiusKm * (180 / Math.PI);
        double dLon = distance / (earthRadiusKm * Math.cos(Math.PI * currentLat / 180)) * (180 / Math.PI);

        currentLat += dLat * Math.cos(direction);
        currentLon += dLon * Math.sin(direction);

        this.locationData = new LocationData(currentLat, currentLon, speedKmh, direction);
    }

}