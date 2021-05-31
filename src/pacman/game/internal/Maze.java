package pacman.game.internal;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
//import java.util.EnumMap;
//import pacman.game.Constants.MOVE;

import static pacman.game.Constants.*;

public final class Maze {
	public AStar astar;
	public int[] shortestPathDistances, pillIndices, powerPillIndices, junctionIndices;
	public int initialPacManNodeIndex, lairNodeIndex, initialGhostNodeIndex;
	public Node[] graph;
	public String name;

	public Maze(int index) {
		loadNodes(nodeNames[index]);
		loadDistances(distNames[index]);

		astar = new AStar();
		astar.createGraph(graph);
	}

	private void loadNodes(String fileName) {
		try {
			@SuppressWarnings("resource")
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(pathMazes + System.getProperty("file.separator") + fileName + ".txt")));
			String input = br.readLine();

			String[] pr = input.split("\t");

			this.name = pr[0];
			this.initialPacManNodeIndex = Integer.parseInt(pr[1]);
			this.lairNodeIndex = Integer.parseInt(pr[2]);
			this.initialGhostNodeIndex = Integer.parseInt(pr[3]);
			this.graph = new Node[Integer.parseInt(pr[4])];
			this.pillIndices = new int[Integer.parseInt(pr[5])];
			this.powerPillIndices = new int[Integer.parseInt(pr[6])];
			this.junctionIndices = new int[Integer.parseInt(pr[7])];

			int nodeIndex = 0;
			int pillIndex = 0;
			int powerPillIndex = 0;
			int junctionIndex = 0;

			input = br.readLine();

			while (input != null) {
				String[] nd = input.split("\t");

				Node node = new Node(Integer.parseInt(nd[0]), Integer.parseInt(nd[1]), Integer.parseInt(nd[2]),
						Integer.parseInt(nd[7]), Integer.parseInt(nd[8]), new int[] { Integer.parseInt(nd[3]),
								Integer.parseInt(nd[4]), Integer.parseInt(nd[5]), Integer.parseInt(nd[6]) });

				graph[nodeIndex++] = node;

				if (node.pillIndex >= 0)
					pillIndices[pillIndex++] = node.nodeIndex;
				else if (node.powerPillIndex >= 0)
					powerPillIndices[powerPillIndex++] = node.nodeIndex;

				if (node.numNeighbouringNodes > 2)
					junctionIndices[junctionIndex++] = node.nodeIndex;

				input = br.readLine();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private void loadDistances(String fileName) {
		this.shortestPathDistances = new int[((graph.length * (graph.length - 1)) / 2) + graph.length];

		try {
			@SuppressWarnings("resource")
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(pathDistances + System.getProperty("file.separator") + fileName)));
			String input = br.readLine();

			int index = 0;

			while (input != null) {
				shortestPathDistances[index++] = Integer.parseInt(input);
				input = br.readLine();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}