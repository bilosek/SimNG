package pl.simNG;

import java.util.Random;

public abstract class SimUnit {
    protected String name;
    protected String type;
    protected Integer viewRange;
    protected Integer shotRange;
    protected Integer speed;
    protected Integer initialUnits;
    protected Integer activeUnits;
    protected Integer initialAmmunition;
    protected Integer currentAmmunition;
    protected double hitProbabilityMin;
    protected double hitProbabilityMax;
    protected double destructionProbabilityMin;
    protected double destructionProbabilityMax;
    protected double fireIntensity;
    protected double criticalLevel;

    private SimGroup parent = null;
    Random random = new Random();

    public SimUnit(String name, String type, Integer viewRange, Integer shotRange, Integer speed, Integer initialUnits, Integer initialAmmunition, double hitProbabilityMin, double hitProbabilityMax, double destructionProbabilityMin , double destructionProbabilityMax, double fireIntensity) {
        if (name == null || type == null || viewRange == null || shotRange == null || speed == null || initialUnits == null || initialAmmunition == null ) {
            throw new IllegalArgumentException("Wszystkie pola muszą być wypełnione.");
        }
        this.name = name;
        this.type = type;
        this.viewRange = viewRange;
        this.shotRange = shotRange;
        this.speed = speed;
        this.initialUnits = initialUnits;
        this.activeUnits = initialUnits;
        this.initialAmmunition = initialAmmunition;
        this.currentAmmunition = initialAmmunition;
        this.hitProbabilityMin = hitProbabilityMin;
        this.hitProbabilityMax = hitProbabilityMax;
        this.destructionProbabilityMin = destructionProbabilityMin;
        this.destructionProbabilityMax = destructionProbabilityMax;
        this.fireIntensity = fireIntensity;
        this.criticalLevel = 0.3;
    }

    public SimGroup getParent() {
        return parent;
    }

    //Jednostka zostaje zniszczona
    public boolean isDestroyed() {
        return activeUnits <= 0;
    }

    //Sprawdza, czy podana pozycja znajduje się w zasięgu strzału jednostki
    public boolean inShotRange(SimPosition position){
        return shotRange >= this.getParent().getPosition().distanceTo(position);
    }

    //Ustawia grupę rodzica dla jednostki.
    public void setParent(SimGroup parent) {
        this.parent = parent;
    }

    //Prawdopodobieństwo trafienia
    public double calculateHitProbability(String targetType, double distance) {
        double targetSizeModifier = getTargetSizeModifier(targetType);
        double hitProbabilityBase = hitProbabilityMin + random.nextDouble() * (hitProbabilityMax - hitProbabilityMin);
        double distanceFactor = 1.0 / (1.0 + distance / shotRange);
        double hitProbability = hitProbabilityBase * targetSizeModifier * distanceFactor;
        return Math.max(0.0, Math.min(1.0, hitProbability));
    }

    //Modyfikator wielkości celu wpływający na prawdopodobieństwo jego trafienia
    private double getTargetSizeModifier(String targetType) {
        switch (targetType.toLowerCase()) {
            case "tank":
                return 1.2;
            case "artillery":
                return 1.1;
            case "combat vehicle":
                return 1.0;
            case "soldier":
                return 0.8;
            default:
                return 1.0;
        }
    }

    //Prawdopodobieństwo zniszczenia celu pod warunkiem trafienia
    public double calculateDestructionProbability(SimUnit attacker, String targetType) {
        double destructionModifier = getDestructionModifier(targetType);

        double destructionProbabilityBase = attacker.getDestructionProbabilityMin() +
                random.nextDouble() * (attacker.getDestructionProbabilityMax() - attacker.getDestructionProbabilityMin());

        double destructionProbability = destructionProbabilityBase * destructionModifier;
        return Math.max(0.0, Math.min(1.0, destructionProbability));
    }

    //Modyfikator trudności zniszczenia celu
    private double getDestructionModifier(String targetType) {
        switch (targetType.toLowerCase()) {
            case "tank":
                return 0.2;
            case "artillery":
                return 0.5;
            case "combat vehicle":
                return 0.7;
            case "soldier":
                return 1.0;
            default:
                return 0.5;
        }
    }

    //Setters
    public void setActiveUnits(Integer initialUnits) {
        this.activeUnits = initialUnits;
    }

    public void setCurrentAmmunition(Integer initialAmmunition) {
        this.currentAmmunition = initialAmmunition;
    }

    //Getters
    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Integer getViewRange() {
        return viewRange;
    }

    public Integer getShotRange() {
        return shotRange;
    }

    public Integer getSpeed() {
        return speed;
    }

    public Integer getInitialUnits() {
        return initialUnits;
    }

    public Integer getActiveUnits() {
        return activeUnits;
    }

    public Integer getInitialAmmunition() {
        return initialAmmunition;
    }

    public Integer getCurrentAmmunition() {
        return currentAmmunition;
    }

    public double getHitProbabilityMin() {
        return hitProbabilityMin;
    }

    public double getHitProbabilityMax() {
        return hitProbabilityMax;
    }

    public double getDestructionProbabilityMin() {
        return destructionProbabilityMin;
    }

    public double getDestructionProbabilityMax() {
        return destructionProbabilityMax;
    }

    public double getFireIntensity() {
        return fireIntensity;
    }

    public double getCriticalLevel() {
        return criticalLevel;
    }
}

