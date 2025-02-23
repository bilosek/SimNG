package pl.simNG;

import pl.simNG.commands.SimCommand;
import pl.simNG.scheduler.SimExecutionScheduler;

import java.util.*;

/**
 * Abstrakcyjna klasa reprezentująca jednostkę (grupę środków bojowych różnego typu) w symulacji.
 * Grupa może zawierać wiele obiektów typu SimUnit. Posiada też pozycję na mapie.
 */
public abstract class SimGroup extends SimExecutionScheduler {
    /** Numer ID jednostki. */
    public final int id;
    /** Bieżąca pozycja jednostki. */
    protected SimPosition position;
    /** Nazwa jednostki (np. "1st Platoon"). */
    private final String name;

    /** Lista wszystkich środków bojowych. */
    protected final List<SimUnit> units = new ArrayList<>();
    /** Lista zniszczonych środków bojowych. */
    protected final List<SimUnit> destroyedUnits = new ArrayList<>();
    /** Lista widocznych jednostek (w zasięgu wykrycia). */
    protected List<SimGroup> visibleGroups = new ArrayList<>();

    /** Bieżące polecenie (rozkaz). */
    private SimCommand currentCommand = null;
    private boolean workingOnCommand = false;
    /** Trasa po której porusza się jednostka (sekwencja kroków w mapie). */
    protected LinkedList<SimVector2i> route = new LinkedList<>();
    /** Strona do której należy jednostka (BLUFOR, REDFOR). */
    private final SimForceType forceType;
    /** Główny obiekt symulacji (rdzeń symulacji). */
    public SimCore parent = null;
    public SimCommander commander = null;

    protected SimPosition lastAttackerPosition = null;

    private SimPosition nextPosition;
    private SimVector2i moveDirection;
    private static double MOVE_RATE = 30.0;

    static int IdCounter = 0;

    /**
     * Konstruktor jednostki z nazwą, pozycją startową i przynależnością.
     * @param name nazwa grupy
     * @param position pozycja startowa
     * @param forceType typ sił (np. BLUEFOR, OPFOR)
     */
    public SimGroup(String name, SimPosition position, SimForceType forceType) {
        this.id = IdCounter++;
        this.name = name;
        this.position = position;
        this.forceType = forceType;
        this.nextPosition = this.position;
    }

    /**
     * Metoda inicjalizująca (do nadpisania przez klasy dziedziczące).
     */
    public abstract void init();

    /**
     * Czy cała jednostka(grupa) jest zniszczona (czyli czy units jest puste).
     * @return true jeśli nie ma już żywych jednostek.
     */
    public boolean isDestroyed(){
        return units.isEmpty();
    }

    /**
     * Zwraca pozycję jednostki.
     * @return SimPosition
     */
    public SimPosition getPosition() {
        return position;
    }

    /**
     * Zwraca nazwę jednostki.
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Dodaje środek bojowy (SimUnit) do jednostki i ustawia jej parent na this.
     * @param unit jednostka
     */
    public void addUnit(SimUnit unit) {
        unit.setParent(this);
        units.add(unit);
    }

    /**
     * Usuwa środek bojowy z listy.
     * @param unit jednostka
     */
    public void removeUnit(SimUnit unit) {
        units.remove(unit);
    }

    /**
     * Ustawia bieżące polecenie (komendę).
     * @param command obiekt typu Command
     */
    public void assignCommand(SimCommand command) {
        this.currentCommand = command;
    }

    /**
     * Zwraca maksymalny zasięg widzenia wśród wszystkich środków bojowych.
     * @return maksymalny visibilityRange, lub -1 jeśli brak środków bojowych.
     */
    public Integer getViewRange(){
        Integer maxViewRange = Integer.MIN_VALUE;

        for (SimUnit unit : units) {
            Integer currentViewRange = unit.visibilityRange;
            if (currentViewRange > maxViewRange) {
                maxViewRange = currentViewRange;
            }
        }

        if (maxViewRange == Integer.MIN_VALUE) {
            return -1;
        }

        return maxViewRange;
    }

    /**
     * Zwraca minimalną (najmniejszą) prędkość wśród rodków bojowych
     * (cała jednostka (grupa) porusza się tempem najwolniejszego).
     * @return prędkość jednostki.
     */
    public int getSpeed() {
        return units.stream()
                .map(SimUnit::getSpeed)
                .min(Integer::compareTo)
                .orElse(1);
    }

    /**
     * Zwraca listę środków bojowych (kopię).
     * @return kopia listy units
     */
    public List<SimUnit> getUnits() {
        return new ArrayList<>(units);
    }

    /**
     * Metoda poruszania się o jeden krok z trasy.
     */
    private int countSteps = 0;
    private int initSteps = 0;
    protected void move(){
        double distance = this.position.subtract(this.nextPosition).length();
        if (distance < 0.1 || countSteps <= 0){
            this.position = this.nextPosition;
            SimVector2i direction = route.poll();
            if (direction != null){
                this.nextPosition = this.position.add(direction);
                this.moveDirection = direction;
            }
            this.countSteps = (int) Math.round((MOVE_RATE / this.getSpeed()));
            this.initSteps = this.countSteps;
        }else{
//            this.position = this.position.add(this.moveDirection.getX() * (this.getSpeed() / MOVE_RATE), this.moveDirection.getY() * (this.getSpeed() / MOVE_RATE));
//            this.position = this.position.add(this.moveDirection.getX() * distance * (this.getSpeed() / MOVE_RATE), this.moveDirection.getY() * distance * (this.getSpeed() / MOVE_RATE));
            this.position = this.position.add((double) this.moveDirection.getX() / this.initSteps, (double) this.moveDirection.getY() / this.initSteps);
//            this.position = this.position.add(this.moveDirection);
            this.countSteps--;
        }


    }

    protected void commandLogic(){
        try {
            //Ruch gdy przeciwnik znajduje się w zasięgu widocznosci
            if (!visibleGroups.isEmpty()) {
                SimGroup target = visibleGroups.get(0);
                if (target.isDestroyed()) {
                    visibleGroups.remove(target);
                    if (!route.isEmpty()) {
                        startCurrentCommand();
                    }
                } else {
                    int myMinShootingRange = units.stream()
                            .mapToInt(SimUnit::getShootingRange)
                            .min()
                            .orElse(1);

                    double currentDistance = position.distanceTo(target.getPosition());

                    if (currentDistance > myMinShootingRange-0.5) {
                        route = calculateRouteTo(target.getPosition());
                        workingOnCommand = true;
                    } else {
                        route.clear();
                        workingOnCommand = false;
                    }
                }
                //Ruch gdy zostanie zaatakowany
            } else if (lastAttackerPosition != null) {
                boolean attackerStillExists = parent.getGroups().stream()
                        .anyMatch(group -> group.getPosition().equals(lastAttackerPosition) && !group.isDestroyed());

                if (!attackerStillExists) {
                    lastAttackerPosition = null;
                    route.clear();
                    workingOnCommand = false;
                } else {
                    double currentDistanceToAttacker = position.distanceTo(lastAttackerPosition);

                    int myMinShootingRange = units.stream()
                            .mapToInt(SimUnit::getShootingRange)
                            .min()
                            .orElse(1);

                    if (currentDistanceToAttacker > myMinShootingRange - 0.5) {
                        route = calculateRouteTo(lastAttackerPosition);
                        workingOnCommand = true;
                    } else {
                        route.clear();
                        workingOnCommand = false;
                    }
                }
                //Normalny ruch
            } else {
                switch (this.getCurrentCommand().type()) {
                    case MOVE, ATTACK, DEFEND -> {
                        if (this.getWorkingOnCommand()) {
                            if(this.position.equals(this.getCurrentCommand().data())){
                                this.endCurrentCommand();
                            }
                        }else{
                            this.startCurrentCommand();
                            this.route = calculateRouteTo((SimPosition) this.getCurrentCommand().data());
                        }
                    }
                    default -> {
                    }
                }
            }
        } catch (Exception ignored){}
    }

    /**
     * Usuwa zniszczone środki bojowe (isDestroyed) z listy units i przenosi je do destroyedUnits.
     */
    protected void cleanDestroyedUnits() {
        Iterator<SimUnit> iterator = units.iterator();
        while (iterator.hasNext()) {
            SimUnit unit = iterator.next();
            if (unit.isDestroyed()) {
                destroyedUnits.add(unit);
                iterator.remove();
            }
        }
    }

    /**
     * Aktualizuje listę widocznych jednostek (przekazywana z zewnątrz).
     * @param visibleGroups lista widocznych jednostek
     */
    public final void updateVisibleGroups(List<SimGroup> visibleGroups) {
        this.visibleGroups = visibleGroups;
    }

    /**
     * Oblicza trasę do wybranego punktu (wywołanie mapy z obiektu parent).
     * @param target pozycja docelowa
     * @return LinkedList wektorów przesunięć
     */
    protected LinkedList<SimVector2i> calculateRouteTo(SimPosition target){
        return parent.getMap().calculateRoute(position, target);
    }

    /**
     * Metoda wywoływana, gdy ktoś atakuje środek bojowy w tej jednostce.
     * @param attacker grupa atakująca
     * @param targetUnit konkretny środek bojowy w tej grupie, która otrzymuje obrażenia
     */
    public abstract void applyDamage(SimGroup attacker, SimUnit targetUnit);

    @Override
    public String toString() {
        return "SimGroup{" +
                "name='" + name + '\'' +
                ", pos=" + position +
                '}';
    }

    /**
     * Zwraca typ sił zbrojnych (np. BLUEFOR, OPFOR).
     */
    public SimForceType getForceType() {
        return forceType;
    }

    /**
     * Zwraca aktualnie widoczne jednostki (grupy).
     * @return lista widocznych jednostek SimGroup
     */
    public List<SimGroup> getVisibleGroups() {
        return visibleGroups;
    }

    /**
     * Zwraca aktualną trasę ruchu jednostki (grupy) jako listę wektorów przesunięć.
     * @return lista wektorów przesunięć opisujących trasę
     */
    public List<SimVector2i> getRoute(){
        return this.route;
    }

    protected SimCommand getCurrentCommand(){
        return this.currentCommand;
    }
    protected void startCurrentCommand(){
        if (currentCommand != null){
            this.workingOnCommand = true;
        }
    }
    protected boolean getWorkingOnCommand(){
        return this.workingOnCommand;
    }
    protected void endCurrentCommand(){
        this.currentCommand = null;
        this.workingOnCommand = false;
    }
}
