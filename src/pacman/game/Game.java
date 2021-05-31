package pacman.game;

import java.util.BitSet;
import java.util.EnumMap;
import java.util.Random;
import java.util.Map.Entry;
import pacman.game.Constants.DM;
import pacman.game.Constants.MOVE;
import pacman.game.internal.Ghost;
import pacman.game.internal.Maze;
import pacman.game.internal.Node;
import pacman.game.internal.PacMan;
import pacman.game.internal.PathsCache;

import static pacman.game.Constants.*;

public final class Game {
	// pills stored as bitsets for efficient copying
	private BitSet pills, powerPills;
	// all the game"s variables
	private int mazeIndex, levelCount, currentLevelTime, totalTime, score, ghostEatMultiplier, timeOfLastGlobalReversal;
	private boolean gameOver, pacmanWasEaten, pillWasEaten, powerPillWasEaten;
	private EnumMap<GHOST, Boolean> ghostsEaten;
	// the data relating to pacman and the ghosts are stored in respective data
	// structures for clarity
	private PacMan pacman;
	private EnumMap<GHOST, Ghost> ghosts;

	// mazes are only loaded once since they don"t change over time
	private static Maze[] mazes = new Maze[NUM_MAZES];;

	private Maze currentMaze;

	static {
		for (int i = 0; i < mazes.length; i++)
			mazes[i] = new Maze(i);
	}

	public static PathsCache[] caches = new PathsCache[NUM_MAZES];

	static {
		for (int i = 0; i < mazes.length; i++) {
			caches[i] = new PathsCache(i);
		}
	}

	private Random rnd;
	private long seed;

	public Game(long seed) {
		this.seed = seed;
		rnd = new Random(seed);

		_init(0);
	}

	public Game(long seed, int initialMaze) {
		this.seed = seed;
		rnd = new Random(seed);

		_init(initialMaze);
	}

	private Game() {
	}

	private void _init(int initialMaze) {
		mazeIndex = initialMaze;
		score = currentLevelTime = levelCount = totalTime = 0;
		ghostEatMultiplier = 1;
		gameOver = false;
		timeOfLastGlobalReversal = -1;
		pacmanWasEaten = false;
		pillWasEaten = false;
		powerPillWasEaten = false;

		ghostsEaten = new EnumMap<GHOST, Boolean>(GHOST.class);

		for (GHOST ghost : GHOST.values())
			ghostsEaten.put(ghost, false);

		_setPills(currentMaze = mazes[mazeIndex]);
		_initGhosts();

		pacman = new PacMan(currentMaze.initialPacManNodeIndex, MOVE.LEFT, NUM_LIVES, false);
	}

	private void _newLevelReset() {
		mazeIndex = ++mazeIndex % NUM_MAZES;
		levelCount++;
		currentMaze = mazes[mazeIndex];

		currentLevelTime = 0;
		ghostEatMultiplier = 1;

		_setPills(currentMaze);
		_levelReset();
	}

	private void _levelReset() {
		ghostEatMultiplier = 1;

		_initGhosts();

		pacman.currentNodeIndex = currentMaze.initialPacManNodeIndex;
		pacman.lastMoveMade = MOVE.LEFT;
	}

	private void _setPills(Maze maze) {
		pills = new BitSet(currentMaze.pillIndices.length);
		pills.set(0, currentMaze.pillIndices.length);
		powerPills = new BitSet(currentMaze.powerPillIndices.length);
		powerPills.set(0, currentMaze.powerPillIndices.length);
	}

	/**
	 * _init ghosts.
	 */
	private void _initGhosts() {
		ghosts = new EnumMap<GHOST, Ghost>(GHOST.class);

		for (GHOST ghostType : GHOST.values())
			ghosts.put(ghostType, new Ghost(ghostType, currentMaze.lairNodeIndex, 0,
					(int) (ghostType.initialLairTime * (Math.pow(LAIR_REDUCTION, levelCount % LEVEL_RESET_REDUCTION))),
					MOVE.NEUTRAL));
	}

	public String getGameState() {
		StringBuilder sb = new StringBuilder();

		sb.append(mazeIndex + "," + totalTime + "," + score + "," + currentLevelTime + "," + levelCount + ","
				+ pacman.currentNodeIndex + "," + pacman.lastMoveMade + "," + pacman.numberOfLivesRemaining + ","
				+ pacman.hasReceivedExtraLife + ",");

		for (Ghost ghost : ghosts.values())
			sb.append(ghost.currentNodeIndex + "," + ghost.edibleTime + "," + ghost.lairTime + "," + ghost.lastMoveMade
					+ ",");

		for (int i = 0; i < currentMaze.pillIndices.length; i++)
			if (pills.get(i))
				sb.append("1");
			else
				sb.append("0");

		sb.append(",");

		for (int i = 0; i < currentMaze.powerPillIndices.length; i++)
			if (powerPills.get(i))
				sb.append("1");
			else
				sb.append("0");

		sb.append(",");
		sb.append(timeOfLastGlobalReversal);
		sb.append(",");
		sb.append(pacmanWasEaten);
		sb.append(",");

		for (GHOST ghost : GHOST.values()) {
			sb.append(ghostsEaten.get(ghost));
			sb.append(",");
		}

		sb.append(pillWasEaten);
		sb.append(",");
		sb.append(powerPillWasEaten);

		return sb.toString();
	}

	public void setGameState(String gameState) {
		String[] values = gameState.split(",");

		int index = 0;

		mazeIndex = Integer.parseInt(values[index++]);
		totalTime = Integer.parseInt(values[index++]);
		score = Integer.parseInt(values[index++]);
		currentLevelTime = Integer.parseInt(values[index++]);
		levelCount = Integer.parseInt(values[index++]);

		pacman = new PacMan(Integer.parseInt(values[index++]), MOVE.valueOf(values[index++]),
				Integer.parseInt(values[index++]), Boolean.parseBoolean(values[index++]));

		ghosts = new EnumMap<GHOST, Ghost>(GHOST.class);

		for (GHOST ghostType : GHOST.values())
			ghosts.put(ghostType,
					new Ghost(ghostType, Integer.parseInt(values[index++]), Integer.parseInt(values[index++]),
							Integer.parseInt(values[index++]), MOVE.valueOf(values[index++])));

		_setPills(currentMaze = mazes[mazeIndex]);

		for (int i = 0; i < values[index].length(); i++)
			if (values[index].charAt(i) == '1')
				pills.set(i);
			else
				pills.clear(i);

		index++;

		for (int i = 0; i < values[index].length(); i++)
			if (values[index].charAt(i) == '1')
				powerPills.set(i);
			else
				powerPills.clear(i);

		timeOfLastGlobalReversal = Integer.parseInt(values[++index]);
		pacmanWasEaten = Boolean.parseBoolean(values[++index]);

		ghostsEaten = new EnumMap<GHOST, Boolean>(GHOST.class);

		for (GHOST ghost : GHOST.values())
			ghostsEaten.put(ghost, Boolean.parseBoolean(values[++index]));

		pillWasEaten = Boolean.parseBoolean(values[++index]);
		powerPillWasEaten = Boolean.parseBoolean(values[++index]);
	}

	public Game copy() {
		Game copy = new Game();

		copy.seed = seed;
		copy.rnd = new Random(seed);
		copy.currentMaze = currentMaze;
		copy.pills = (BitSet) pills.clone();
		copy.powerPills = (BitSet) powerPills.clone();
		copy.mazeIndex = mazeIndex;
		copy.levelCount = levelCount;
		copy.currentLevelTime = currentLevelTime;
		copy.totalTime = totalTime;
		copy.score = score;
		copy.ghostEatMultiplier = ghostEatMultiplier;
		copy.gameOver = gameOver;
		copy.timeOfLastGlobalReversal = timeOfLastGlobalReversal;
		copy.pacmanWasEaten = pacmanWasEaten;
		copy.pillWasEaten = pillWasEaten;
		copy.powerPillWasEaten = powerPillWasEaten;
		copy.pacman = pacman.copy();

		copy.ghostsEaten = new EnumMap<GHOST, Boolean>(GHOST.class);
		copy.ghosts = new EnumMap<GHOST, Ghost>(GHOST.class);

		for (GHOST ghostType : GHOST.values()) {
			copy.ghosts.put(ghostType, ghosts.get(ghostType).copy());
			copy.ghostsEaten.put(ghostType, ghostsEaten.get(ghostType));
		}

		return copy;
	}

	// ///////////////////////////////////////////////////////////////////////////
	// ///////////////////////// Game-engine //////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////

	public void advanceGame(MOVE pacManMove, EnumMap<GHOST, MOVE> ghostMoves) {
		updatePacMan(pacManMove);
		updateGhosts(ghostMoves);
		updateGame();
	}

	public void advanceGameWithoutReverse(MOVE pacManMove, EnumMap<GHOST, MOVE> ghostMoves) {
		updatePacMan(pacManMove);
		updateGhostsWithoutReverse(ghostMoves);
		updateGame();
	}

	public void advanceGameWithForcedReverse(MOVE pacManMove, EnumMap<GHOST, MOVE> ghostMoves) {
		updatePacMan(pacManMove);
		updateGhostsWithForcedReverse(ghostMoves);
		updateGame();
	}

	public void advanceGameWithPowerPillReverseOnly(MOVE pacManMove, EnumMap<GHOST, MOVE> ghostMoves) {
		updatePacMan(pacManMove);

		if (powerPillWasEaten)
			updateGhostsWithForcedReverse(ghostMoves);
		else
			updateGhostsWithoutReverse(ghostMoves);

		updateGame();
	}

	public void updatePacMan(MOVE pacManMove) {
		_updatePacMan(pacManMove); // move pac-man
		_eatPill(); // eat a pill
		_eatPowerPill(); // eat a power pill
	}

	public void updateGhosts(EnumMap<GHOST, MOVE> ghostMoves) {
		ghostMoves = _completeGhostMoves(ghostMoves);

		if (!_reverseGhosts(ghostMoves, false))
			_updateGhosts(ghostMoves);
	}

	public void updateGhostsWithoutReverse(EnumMap<GHOST, MOVE> ghostMoves) {
		ghostMoves = _completeGhostMoves(ghostMoves);
		_updateGhosts(ghostMoves);
	}

	public void updateGhostsWithForcedReverse(EnumMap<GHOST, MOVE> ghostMoves) {
		ghostMoves = _completeGhostMoves(ghostMoves);
		_reverseGhosts(ghostMoves, true);
	}

	public void updateGame() {
		_feast(); // ghosts eat pac-man or vice versa
		_updateLairTimes();
		_updatePacManExtraLife();

		totalTime++;
		currentLevelTime++;

		_checkLevelState(); // check if level/game is over
	}

	public void updateGame(boolean feast, boolean updateLairTimes, boolean updateExtraLife, boolean updateTotalTime,
			boolean updateLevelTime) {
		if (feast)
			_feast(); // ghosts eat pac-man or vice versa
		if (updateLairTimes)
			_updateLairTimes();
		if (updateExtraLife)
			_updatePacManExtraLife();

		if (updateTotalTime)
			totalTime++;
		if (updateLevelTime)
			currentLevelTime++;

		_checkLevelState(); // check if level/game is over
	}

	private void _updateLairTimes() {
		for (Ghost ghost : ghosts.values())
			if (ghost.lairTime > 0)
				if (--ghost.lairTime == 0)
					ghost.currentNodeIndex = currentMaze.initialGhostNodeIndex;
	}

	private void _updatePacManExtraLife() {
		if (!pacman.hasReceivedExtraLife && score >= EXTRA_LIFE_SCORE) // award
																		// 1
																		// extra
																		// life
																		// at
																		// 10000
																		// points
		{
			pacman.hasReceivedExtraLife = true;
			pacman.numberOfLivesRemaining++;
		}
	}

	private void _updatePacMan(MOVE move) {
		pacman.lastMoveMade = _correctPacManDir(move);
		pacman.currentNodeIndex = pacman.lastMoveMade == MOVE.NEUTRAL ? pacman.currentNodeIndex
				: currentMaze.graph[pacman.currentNodeIndex].neighbourhood.get(pacman.lastMoveMade);
	}

	private MOVE _correctPacManDir(MOVE direction) {
		Node node = currentMaze.graph[pacman.currentNodeIndex];

		// direction is correct, return it
		if (node.neighbourhood.containsKey(direction))
			return direction;
		else {
			// try to use previous direction (i.e., continue in the same
			// direction)
			if (node.neighbourhood.containsKey(pacman.lastMoveMade))
				return pacman.lastMoveMade;
			// else stay put
			else
				return MOVE.NEUTRAL;
		}
	}

	private void _updateGhosts(EnumMap<GHOST, MOVE> moves) {
		for (Entry<GHOST, MOVE> entry : moves.entrySet()) {
			Ghost ghost = ghosts.get(entry.getKey());

			if (ghost.lairTime == 0) {
				if (ghost.edibleTime == 0 || ghost.edibleTime % GHOST_SPEED_REDUCTION != 0) {
					ghost.lastMoveMade = _checkGhostDir(ghost, entry.getValue());
					moves.put(entry.getKey(), ghost.lastMoveMade);
					ghost.currentNodeIndex = currentMaze.graph[ghost.currentNodeIndex].neighbourhood
							.get(ghost.lastMoveMade);
				}
			}
		}
	}

	private EnumMap<GHOST, MOVE> _completeGhostMoves(EnumMap<GHOST, MOVE> moves) {
		if (moves == null) {
			moves = new EnumMap<GHOST, MOVE>(GHOST.class);

			for (GHOST ghostType : GHOST.values())
				moves.put(ghostType, ghosts.get(ghostType).lastMoveMade);
		}

		if (moves.size() < NUM_GHOSTS)
			for (GHOST ghostType : GHOST.values())
				if (!moves.containsKey(ghostType))
					moves.put(ghostType, MOVE.NEUTRAL);

		return moves;
	}

	private MOVE _checkGhostDir(Ghost ghost, MOVE direction) {
		// Gets the neighbours of the node with the node that would correspond
		// to reverse removed
		Node node = currentMaze.graph[ghost.currentNodeIndex];

		// The direction is possible and not opposite to the previous direction
		// of that ghost
		if (node.neighbourhood.containsKey(direction) && direction != ghost.lastMoveMade.opposite())
			return direction;
		else {
			if (node.neighbourhood.containsKey(ghost.lastMoveMade))
				return ghost.lastMoveMade;
			else {
				MOVE[] moves = node.allPossibleMoves.get(ghost.lastMoveMade);
				return moves[rnd.nextInt(moves.length)];
			}
		}
	}

	private void _eatPill() {
		pillWasEaten = false;

		int pillIndex = currentMaze.graph[pacman.currentNodeIndex].pillIndex;

		if (pillIndex >= 0 && pills.get(pillIndex)) {
			score += PILL;
			pills.clear(pillIndex);
			pillWasEaten = true;
		}
	}

	private void _eatPowerPill() {
		powerPillWasEaten = false;

		int powerPillIndex = currentMaze.graph[pacman.currentNodeIndex].powerPillIndex;

		if (powerPillIndex >= 0 && powerPills.get(powerPillIndex)) {
			score += POWER_PILL;
			ghostEatMultiplier = 1;
			powerPills.clear(powerPillIndex);

			int newEdibleTime = (int) (EDIBLE_TIME
					* (Math.pow(EDIBLE_TIME_REDUCTION, levelCount % LEVEL_RESET_REDUCTION)));

			for (Ghost ghost : ghosts.values())
				if (ghost.lairTime == 0)
					ghost.edibleTime = newEdibleTime;
				else
					ghost.edibleTime = 0;

			powerPillWasEaten = true;
		}
	}

	private boolean _reverseGhosts(EnumMap<GHOST, MOVE> moves, boolean force) {
		boolean reversed = false;
		boolean globalReverse = false;

		if (Math.random() < GHOST_REVERSAL)
			globalReverse = true;

		for (Entry<GHOST, MOVE> entry : moves.entrySet()) {
			Ghost ghost = ghosts.get(entry.getKey());

			if (currentLevelTime > 1 && ghost.lairTime == 0 && ghost.lastMoveMade != MOVE.NEUTRAL) {
				if (force || (powerPillWasEaten || globalReverse)) {
					ghost.lastMoveMade = ghost.lastMoveMade.opposite();
					ghost.currentNodeIndex = currentMaze.graph[ghost.currentNodeIndex].neighbourhood
							.get(ghost.lastMoveMade);
					reversed = true;
					timeOfLastGlobalReversal = totalTime;
				}
			}
		}

		return reversed;
	}

	private void _feast() {
		pacmanWasEaten = false;

		for (GHOST ghost : GHOST.values())
			ghostsEaten.put(ghost, false);

		for (Ghost ghost : ghosts.values()) {
			int distance = getShortestPathDistance(pacman.currentNodeIndex, ghost.currentNodeIndex);

			if (distance <= EAT_DISTANCE && distance != -1) {
				if (ghost.edibleTime > 0) // pac-man eats ghost
				{
					score += GHOST_EAT_SCORE * ghostEatMultiplier;
					ghostEatMultiplier *= 2;
					ghost.edibleTime = 0;
					ghost.lairTime = (int) (COMMON_LAIR_TIME
							* (Math.pow(LAIR_REDUCTION, levelCount % LEVEL_RESET_REDUCTION)));
					ghost.currentNodeIndex = currentMaze.lairNodeIndex;
					ghost.lastMoveMade = MOVE.NEUTRAL;

					ghostsEaten.put(ghost.type, true);
				} else // ghost eats pac-man
				{
					pacman.numberOfLivesRemaining--;
					pacmanWasEaten = true;

					if (pacman.numberOfLivesRemaining <= 0)
						gameOver = true;
					else
						_levelReset();

					return;
				}
			}
		}

		for (Ghost ghost : ghosts.values())
			if (ghost.edibleTime > 0)
				ghost.edibleTime--;
	}

	private void _checkLevelState() {
		// put a cap on the total time a game can be played for
		if (totalTime + 1 > MAX_TIME) {
			gameOver = true;
			score += pacman.numberOfLivesRemaining * AWARD_LIFE_LEFT;
		}
		// if all pills have been eaten or the time is up...
		else if ((pills.isEmpty() && powerPills.isEmpty()) || currentLevelTime >= LEVEL_LIMIT)
			_newLevelReset();
	}

	// ///////////////////////////////////////////////////////////////////////////
	// ///////////////// Query Methods (return only) ///////////////////////////
	// ///////////////////////////////////////////////////////////////////////////

	public boolean wasPacManEaten() {
		return pacmanWasEaten;
	}

	public boolean wasGhostEaten(GHOST ghost) {
		return ghostsEaten.get(ghost);
	}

	public int getNumGhostsEaten() {
		int count = 0;

		for (GHOST ghost : GHOST.values())
			if (ghostsEaten.get(ghost))
				count++;

		return count;
	}

	public boolean wasPillEaten() {
		return pillWasEaten;
	}

	public boolean wasPowerPillEaten() {
		return powerPillWasEaten;
	}

	public int getTimeOfLastGlobalReversal() {
		return timeOfLastGlobalReversal;
	}

	public boolean gameOver() {
		return gameOver;
	}

	public Maze getCurrentMaze() {
		return currentMaze;
	}

	public int getNodeXCood(int nodeIndex) {
		return currentMaze.graph[nodeIndex].x;
	}

	public int getNodeYCood(int nodeIndex) {
		return currentMaze.graph[nodeIndex].y;
	}

	public int getMazeIndex() {
		return mazeIndex;
	}

	public int getCurrentLevel() {
		return levelCount;
	}

	public int getNumberOfNodes() {
		return currentMaze.graph.length;
	}

	public int getGhostCurrentEdibleScore() {
		return GHOST_EAT_SCORE * ghostEatMultiplier;
	}

	public int getGhostInitialNodeIndex() {
		return currentMaze.initialGhostNodeIndex;
	}

	public boolean isPillStillAvailable(int pillIndex) {
		return pills.get(pillIndex);
	}

	public boolean isPowerPillStillAvailable(int powerPillIndex) {
		return powerPills.get(powerPillIndex);
	}

	public int getPillIndex(int nodeIndex) {
		return currentMaze.graph[nodeIndex].pillIndex;
	}

	public int getPowerPillIndex(int nodeIndex) {
		return currentMaze.graph[nodeIndex].powerPillIndex;
	}

	public int[] getJunctionIndices() {
		return currentMaze.junctionIndices;
	}

	public int[] getPillIndices() {
		return currentMaze.pillIndices;
	}

	public int[] getPowerPillIndices() {
		return currentMaze.powerPillIndices;
	}

	public int getPacmanCurrentNodeIndex() {
		return pacman.currentNodeIndex;
	}

	public MOVE getPacmanLastMoveMade() {
		return pacman.lastMoveMade;
	}

	public int getPacmanNumberOfLivesRemaining() {
		return pacman.numberOfLivesRemaining;
	}

	public int getGhostCurrentNodeIndex(GHOST ghostType) {
		return ghosts.get(ghostType).currentNodeIndex;
	}

	public MOVE getGhostLastMoveMade(GHOST ghostType) {
		return ghosts.get(ghostType).lastMoveMade;
	}

	public int getGhostEdibleTime(GHOST ghostType) {
		return ghosts.get(ghostType).edibleTime;
	}

	public boolean isGhostEdible(GHOST ghostType) {
		return ghosts.get(ghostType).edibleTime > 0;
	}

	public int getScore() {
		return score;
	}

	public int getCurrentLevelTime() {
		return currentLevelTime;
	}

	public int getTotalTime() {
		return totalTime;
	}

	public int getNumberOfPills() {
		return currentMaze.pillIndices.length;
	}

	public int getNumberOfPowerPills() {
		return currentMaze.powerPillIndices.length;
	}

	public int getNumberOfActivePills() {
		return pills.cardinality();
	}

	public int getNumberOfActivePowerPills() {
		return powerPills.cardinality();
	}

	public int getGhostLairTime(GHOST ghostType) {
		return ghosts.get(ghostType).lairTime;
	}

	public int[] getActivePillsIndices() {
		int[] indices = new int[pills.cardinality()];

		int index = 0;

		for (int i = 0; i < currentMaze.pillIndices.length; i++)
			if (pills.get(i))
				indices[index++] = currentMaze.pillIndices[i];

		return indices;
	}

	public int[] getActivePowerPillsIndices() {
		int[] indices = new int[powerPills.cardinality()];

		int index = 0;

		for (int i = 0; i < currentMaze.powerPillIndices.length; i++)
			if (powerPills.get(i))
				indices[index++] = currentMaze.powerPillIndices[i];

		return indices;
	}

	public boolean doesGhostRequireAction(GHOST ghostType) {
		// inlcude neutral here for the unique case where the ghost just left
		// the lair
		return ((isJunction(ghosts.get(ghostType).currentNodeIndex)
				|| (ghosts.get(ghostType).lastMoveMade == MOVE.NEUTRAL)
						&& ghosts.get(ghostType).currentNodeIndex == currentMaze.initialGhostNodeIndex)
				&& (ghosts.get(ghostType).edibleTime == 0
						|| ghosts.get(ghostType).edibleTime % GHOST_SPEED_REDUCTION != 0));
	}

	public boolean isJunction(int nodeIndex) {
		return currentMaze.graph[nodeIndex].numNeighbouringNodes > 2;
	}

	public MOVE[] getPossibleMoves(int nodeIndex) {
		return currentMaze.graph[nodeIndex].allPossibleMoves.get(MOVE.NEUTRAL);
	}

	public MOVE[] getPossibleMoves(int nodeIndex, MOVE lastModeMade) {
		return currentMaze.graph[nodeIndex].allPossibleMoves.get(lastModeMade);
	}

	public int[] getNeighbouringNodes(int nodeIndex) {
		return currentMaze.graph[nodeIndex].allNeighbouringNodes.get(MOVE.NEUTRAL);
	}

	public int[] getNeighbouringNodes(int nodeIndex, MOVE lastModeMade) {
		return currentMaze.graph[nodeIndex].allNeighbouringNodes.get(lastModeMade);
	}

	public int getNeighbour(int nodeIndex, MOVE moveToBeMade) {
		Integer neighbour = currentMaze.graph[nodeIndex].neighbourhood.get(moveToBeMade);

		return neighbour == null ? -1 : neighbour;
	}

	public MOVE getMoveToMakeToReachDirectNeighbour(int currentNodeIndex, int neighbourNodeIndex) {
		for (MOVE move : MOVE.values()) {
			if (currentMaze.graph[currentNodeIndex].neighbourhood.containsKey(move)
					&& currentMaze.graph[currentNodeIndex].neighbourhood.get(move) == neighbourNodeIndex) {
				return move;
			}
		}

		return null;
	}

	// ///////////////////////////////////////////////////////////////////////////
	// ///////////////// Helper Methods (computational) ////////////////////////
	// ///////////////////////////////////////////////////////////////////////////

	public int getShortestPathDistance(int fromNodeIndex, int toNodeIndex) {
		if (fromNodeIndex == toNodeIndex)
			return 0;
		else if (fromNodeIndex < toNodeIndex)
			return currentMaze.shortestPathDistances[((toNodeIndex * (toNodeIndex + 1)) / 2) + fromNodeIndex];
		else
			return currentMaze.shortestPathDistances[((fromNodeIndex * (fromNodeIndex + 1)) / 2) + toNodeIndex];
	}

	public double getEuclideanDistance(int fromNodeIndex, int toNodeIndex) {
		return Math.sqrt(Math.pow(currentMaze.graph[fromNodeIndex].x - currentMaze.graph[toNodeIndex].x, 2)
				+ Math.pow(currentMaze.graph[fromNodeIndex].y - currentMaze.graph[toNodeIndex].y, 2));
	}

	public int getManhattanDistance(int fromNodeIndex, int toNodeIndex) {
		return (int) (Math.abs(currentMaze.graph[fromNodeIndex].x - currentMaze.graph[toNodeIndex].x)
				+ Math.abs(currentMaze.graph[fromNodeIndex].y - currentMaze.graph[toNodeIndex].y));
	}

	public double getDistance(int fromNodeIndex, int toNodeIndex, DM distanceMeasure) {
		switch (distanceMeasure) {
		case PATH:
			return getShortestPathDistance(fromNodeIndex, toNodeIndex);
		case EUCLID:
			return getEuclideanDistance(fromNodeIndex, toNodeIndex);
		case MANHATTAN:
			return getManhattanDistance(fromNodeIndex, toNodeIndex);
		}

		return -1;
	}

	public double getDistance(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade, DM distanceMeasure) {
		switch (distanceMeasure) {
		case PATH:
			return getApproximateShortestPathDistance(fromNodeIndex, toNodeIndex, lastMoveMade);
		case EUCLID:
			return getEuclideanDistance(fromNodeIndex, toNodeIndex);
		case MANHATTAN:
			return getManhattanDistance(fromNodeIndex, toNodeIndex);
		}

		return -1;
	}

	public int getClosestNodeIndexFromNodeIndex(int fromNodeIndex, int[] targetNodeIndices, DM distanceMeasure) {
		double minDistance = Integer.MAX_VALUE;
		int target = -1;

		for (int i = 0; i < targetNodeIndices.length; i++) {
			double distance = 0;

			distance = getDistance(targetNodeIndices[i], fromNodeIndex, distanceMeasure);

			if (distance < minDistance) {
				minDistance = distance;
				target = targetNodeIndices[i];
			}
		}

		return target;
	}

	public int getFarthestNodeIndexFromNodeIndex(int fromNodeIndex, int[] targetNodeIndices, DM distanceMeasure) {
		double maxDistance = Integer.MIN_VALUE;
		int target = -1;

		for (int i = 0; i < targetNodeIndices.length; i++) {
			double distance = 0;

			distance = getDistance(targetNodeIndices[i], fromNodeIndex, distanceMeasure);

			if (distance > maxDistance) {
				maxDistance = distance;
				target = targetNodeIndices[i];
			}
		}

		return target;
	}

	public MOVE getNextMoveTowardsTarget(int fromNodeIndex, int toNodeIndex, DM distanceMeasure) {
		MOVE move = null;

		double minDistance = Integer.MAX_VALUE;

		for (Entry<MOVE, Integer> entry : currentMaze.graph[fromNodeIndex].neighbourhood.entrySet()) {
			double distance = getDistance(entry.getValue(), toNodeIndex, distanceMeasure);

			if (distance < minDistance) {
				minDistance = distance;
				move = entry.getKey();
			}
		}

		return move;
	}

	public MOVE getNextMoveAwayFromTarget(int fromNodeIndex, int toNodeIndex, DM distanceMeasure) {
		MOVE move = null;

		double maxDistance = Integer.MIN_VALUE;

		for (Entry<MOVE, Integer> entry : currentMaze.graph[fromNodeIndex].neighbourhood.entrySet()) {
			double distance = getDistance(entry.getValue(), toNodeIndex, distanceMeasure);

			if (distance > maxDistance) {
				maxDistance = distance;
				move = entry.getKey();
			}
		}

		return move;
	}

	public MOVE getApproximateNextMoveTowardsTarget(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade,
			DM distanceMeasure) {
		MOVE move = null;

		double minDistance = Integer.MAX_VALUE;

		for (Entry<MOVE, Integer> entry : currentMaze.graph[fromNodeIndex].allNeighbourhoods.get(lastMoveMade)
				.entrySet()) {
			double distance = getDistance(entry.getValue(), toNodeIndex, distanceMeasure);

			if (distance < minDistance) {
				minDistance = distance;
				move = entry.getKey();
			}
		}

		return move;
	}

	public MOVE getApproximateNextMoveAwayFromTarget(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade,
			DM distanceMeasure) {
		MOVE move = null;

		double maxDistance = Integer.MIN_VALUE;

		for (Entry<MOVE, Integer> entry : currentMaze.graph[fromNodeIndex].allNeighbourhoods.get(lastMoveMade)
				.entrySet()) {
			double distance = getDistance(entry.getValue(), toNodeIndex, distanceMeasure);

			if (distance > maxDistance) {
				maxDistance = distance;
				move = entry.getKey();
			}
		}

		return move;
	}

	public MOVE getNextMoveTowardsTarget(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade, DM distanceMeasure) {
		MOVE move = null;

		double minDistance = Integer.MAX_VALUE;

		for (Entry<MOVE, Integer> entry : currentMaze.graph[fromNodeIndex].allNeighbourhoods.get(lastMoveMade)
				.entrySet()) {
			double distance = getDistance(entry.getValue(), toNodeIndex, lastMoveMade, distanceMeasure);

			if (distance < minDistance) {
				minDistance = distance;
				move = entry.getKey();
			}
		}

		return move;
	}

	public MOVE getNextMoveAwayFromTarget(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade, DM distanceMeasure) {
		MOVE move = null;

		double maxDistance = Integer.MIN_VALUE;

		for (Entry<MOVE, Integer> entry : currentMaze.graph[fromNodeIndex].allNeighbourhoods.get(lastMoveMade)
				.entrySet()) {
			double distance = getDistance(entry.getValue(), toNodeIndex, lastMoveMade, distanceMeasure);

			if (distance > maxDistance) {
				maxDistance = distance;
				move = entry.getKey();
			}
		}

		return move;
	}

	public int[] getAStarPath(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade) {
		return getShortestPath(fromNodeIndex, toNodeIndex, lastMoveMade);
	}

	public int[] getShortestPath(int fromNodeIndex, int toNodeIndex) {
		return caches[mazeIndex].getPathFromA2B(fromNodeIndex, toNodeIndex);
	}

	public int[] getApproximateShortestPath(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade) {
		return getShortestPath(fromNodeIndex, toNodeIndex, lastMoveMade);
	}

	public int[] getShortestPath(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade) {
		if (currentMaze.graph[fromNodeIndex].neighbourhood.size() == 0)// lair
			return new int[0];

		return caches[mazeIndex].getPathFromA2B(fromNodeIndex, toNodeIndex, lastMoveMade);
	}

	public int getApproximateShortestPathDistance(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade) {
		return getShortestPathDistance(fromNodeIndex, toNodeIndex, lastMoveMade);
	}

	public int getShortestPathDistance(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade) {
		if (currentMaze.graph[fromNodeIndex].neighbourhood.size() == 0)// lair
			return 0;

		return caches[mazeIndex].getPathDistanceFromA2B(fromNodeIndex, toNodeIndex, lastMoveMade);
	}
}