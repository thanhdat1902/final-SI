package partAModelSI;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;

public class Infected {
    private static int infectedCount = 0;

    private ContinuousSpace<Object> space;
    private Grid<Object> grid;
    private double beta;
    private int N;
    
    public Infected(ContinuousSpace<Object> space, Grid<Object> grid, double beta, int N) {
        this.space = space;
        this.grid = grid;
        this.beta = beta;
        this.N = N;
        incrementInfectedCount();
    }

    @ScheduledMethod(start = 1, interval = 1)
    public void step() {
        // Attempt infection at the current location
        infect();

        // Stop the simulation if no infected agents remain
        if (getInfectedCount() == this.N) {
            stopSimulation();
        }
    }

    public static synchronized void incrementInfectedCount() {
        infectedCount++;
    }

    public static synchronized void decrementInfectedCount() {
        infectedCount--;
    }

    public static synchronized int getInfectedCount() {
        return infectedCount;
    }

    public void stopSimulation() {
        System.out.println("Stopping the simulation: no infected agents left");
        repast.simphony.engine.environment.RunEnvironment.getInstance().endRun();
    }

    public void infect() {
        GridPoint pt = grid.getLocation(this);
        List<Object> susceptible = new ArrayList<>();

        // Collect all susceptible agents at the current location
        for (Object obj : grid.getObjects()) {
            if (obj instanceof Susceptible) {
                susceptible.add(obj);
            }
        }

        // Randomly infect one susceptible agent
        if (!susceptible.isEmpty() && Math.random() < this.beta) {
            Object objToInfect = susceptible.get(RandomHelper.nextIntFromTo(0, susceptible.size() - 1));

            // Convert the selected susceptible agent into an infected agent
            NdPoint spacePt = space.getLocation(objToInfect);
            Context<Object> context = ContextUtils.getContext(objToInfect);
            context.remove(objToInfect);
            Infected infected = new Infected(space, grid, this.beta, this.N);
            context.add(infected);
            space.moveTo(infected, spacePt.getX(), spacePt.getY());
            grid.moveTo(infected, pt.getX(), pt.getY());

            // Add an edge in the infection network
            Network<Object> net = (Network<Object>) context.getProjection("infection network");
            net.addEdge(this, infected);
        }
    }

}