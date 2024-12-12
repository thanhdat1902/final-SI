package networksim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import repast.simphony.context.Context;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.graph.Network;

public class EpidemiologyModelBuilder implements ContextBuilder<Object> {

    @Override
    public Context build(Context<Object> context) {
        context.setId("networksim");
        
        

    	
        // Create network and other required components
        NetworkBuilder<Object> netBuilder = new NetworkBuilder<>("infection network", context, false);
        Network<Object> network = netBuilder.buildNetwork(); // This returns the network you can use

        // Parameters for infected, susceptible, and recovered individuals
        Parameters params = RunEnvironment.getInstance().getParameters();
        double beta = (Double) params.getValue("beta");
        double gamma = (Double) params.getValue("gamma");
        int zombieCount = (Integer) params.getValue("infected_count");
        List<Object> agents = new ArrayList<>();

        for (int i = 0; i < zombieCount; i++) {
        	Infected infected = new Infected(beta, gamma);
            context.add(infected);
            agents.add(infected);
        }

        int humanCount = (Integer) params.getValue("susceptible_count");
		for (int i = 0; i < humanCount; i++) {
			Susceptible susceptible = new Susceptible();
	        context.add(susceptible);
	        agents.add(susceptible);
        }

        int recoveredCount = (Integer) params.getValue("recovered_count");
        for (int i = 0; i < recoveredCount; i++) {
        	Recovered recovered = new Recovered();
            context.add(recovered);
            agents.add(recovered);
        }
        
        String networkType = (String) params.getValue("network_type");
        
        int d = (Integer) params.getValue("average_degree_network");
        double p = (Double) params.getValue("probability_small_world");

    	
        if(networkType.equals("barabasi")) {
        	int m = d/2;
            barabasiAlbertGraph(context, network, agents, m); // Example: 100 nodes, 5 edges per node
        } else if(networkType.equals("smallworld")) {
        	int k = d;
        	wattsStrogatzGraph(context,network, agents, k, p);
        }

        // Generate the Barabasi-Albert or Watts-Strogatz network

        // Optionally, you could add some behavior logic for infection spread or recovery, 
        // but no need for grid/space if you aren't visualizing or using movement.

        if (RunEnvironment.getInstance().isBatch()) {
            RunEnvironment.getInstance().endAt(20);
        }

        return context;
    }
    private List<Object> getRepeatedNodes(Network<Object> network) {
        List<Object> repeatedNodes = new ArrayList<>();

        // Iterate over all nodes in the network
        for (Object node : network.getNodes()) {
            int degree = network.getDegree(node);
            for (int i = 0; i < degree; i++) {
                repeatedNodes.add(node);
            }
        }

        return repeatedNodes;
    }

    private void barabasiAlbertGraph(Context<Object> context, Network<Object> network, List<Object> agents, int m) {
        Random random = new Random();
        List<Object> nodes = new ArrayList<>(agents.subList(0, Math.min(m + 1, agents.size())));
        
        // Create the initial star graph by connecting the first m nodes to the (m+1)th node
        for (int i = 0; i < m; i++) {
            network.addEdge(nodes.get(i), nodes.get(m));
        }

        // Create a list to simulate node degrees based on the edges in the network
        List<Object> repeatedNodes = getRepeatedNodes(network);

        // Add the remaining nodes with preferential attachment
        for (int i = m + 1; i < agents.size(); i++) {
            Object newNode = agents.get(i);
            Set<Object> targets = new HashSet<>();

            // Select m unique targets based on preferential attachment (degree)
            while (targets.size() < m) {
                Object selectedNode = repeatedNodes.get(random.nextInt(repeatedNodes.size()));
                targets.add(selectedNode);
            }

            // Add edges for the new node
            for (Object target : targets) {
                network.addEdge(newNode, target);
            }

            // Update repeatedNodes to reflect the new connections
            repeatedNodes = getRepeatedNodes(network);
        }
    }

    
    private void wattsStrogatzGraph(Context<Object> context, Network<Object> network, List<Object> agents, int k, double p) {
        Random random = new Random();
        int n = agents.size();

        // Step 1: Create a regular ring lattice
        List<Object> nodes = new ArrayList<>(agents);

        // Connect each node to k/2 nearest neighbors in both forward and backward directions
        for (int i = 0; i < n; i++) {
            for (int j = 1; j <= k / 2; j++) {
                int forwardNeighbor = (i + j) % n;
                int backwardNeighbor = (i - j + n) % n;

                network.addEdge(nodes.get(i), nodes.get(forwardNeighbor));
                network.addEdge(nodes.get(i), nodes.get(backwardNeighbor));
            }
        }

        // Step 2: Rewire edges with probability p

        for (int i = 0; i < n; i++) {
            for (int j = 1; j <= k / 2; j++) {
                int forwardNeighbor = (i + j) % n;
                int backwardNeighbor = (i - j + n) % n;

                // Rewire the forward edge with probability p
                if (random.nextDouble() < p) {
                    Object currentNode = nodes.get(i);
                    Object oldForwardTarget = nodes.get(forwardNeighbor);
                    Object newForwardTarget;

                    // Pick a new target node that is not the current node and not already connected
                    do {
                        newForwardTarget = nodes.get(random.nextInt(n));
                    } while (newForwardTarget == currentNode || network.isAdjacent(currentNode, newForwardTarget));

                    // Remove the old forward edge and add the new one
                    network.removeEdge(network.getEdge(currentNode, oldForwardTarget));
                    network.addEdge(currentNode, newForwardTarget);
                }

                // Rewire the backward edge with probability p
                if (random.nextDouble() < p) {
                    Object currentNode = nodes.get(i);
                    Object oldBackwardTarget = nodes.get(backwardNeighbor);
                    Object newBackwardTarget;

                    // Pick a new target node that is not the current node and not already connected
                    do {
                        newBackwardTarget = nodes.get(random.nextInt(n));
                    } while (newBackwardTarget == currentNode || network.isAdjacent(currentNode, newBackwardTarget));

                    // Remove the old backward edge and add the new one
                    network.removeEdge(network.getEdge(currentNode, oldBackwardTarget));
                    network.addEdge(currentNode, newBackwardTarget);
                }
            }
        }
    }




}
