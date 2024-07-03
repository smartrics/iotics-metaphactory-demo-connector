package smartrics.iotics.samples.cars;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MovingCar {
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


    public MovingCar(double initialLat, double initialLon, int updatePeriodSec) {
        this.rand = new Random();
        this.currentLat = initialLat;
        this.currentLon = initialLon;
        this.maxDistance = rand.nextDouble(5, 20);
        this.distanceCovered = 0.0;
        this.returning = false;
        this.speedKmh = rand.nextDouble(45.0, 55.0);
        this.direction = rand.nextDouble() * 2 * Math.PI; // Random direction in radians
        this.lastUpdateTime = LocalDateTime.now();
        update();
        try(ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)) {
            scheduler.scheduleAtFixedRate(this::update, 0, updatePeriodSec, TimeUnit.SECONDS);
        }
    }

    private void update() {
        updatePosition();
        updateOperationalStatus();
    }

    public OperationalStatus currentOperationalStatus() {
        return operationalStatus;
    }

    public LocationData currentLocationData() {
        return locationData;
    }

    public double latitude() {
        return currentLat;
    }

    public double longitude() {
        return currentLon;
    }

    public double direction() {
        return direction;
    }

    public double speedKmk() {
        return speedKmh;
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