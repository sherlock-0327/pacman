package pacman.controllers.examples;

import java.awt.Color;
import pacman.controllers.Controller;
import pacman.game.Game;
import pacman.game.GameView;
import pacman.game.Constants.DM;

import static pacman.game.Constants.*;

public final class NearestPillPacManVS extends Controller<MOVE> {

	@SuppressWarnings("deprecation")
	public MOVE getMove(Game game, long timeDue) {
		int currentNodeIndex = game.getPacmanCurrentNodeIndex();
		int[] activePills = game.getActivePillsIndices();
		int[] activePowerPills = game.getActivePowerPillsIndices();
		int[] targetNodeIndices = new int[activePills.length + activePowerPills.length];

		for (int i = 0; i < activePills.length; i++)
			targetNodeIndices[i] = activePills[i];

		for (int i = 0; i < activePowerPills.length; i++)
			targetNodeIndices[activePills.length + i] = activePowerPills[i];

		int nearest = game.getClosestNodeIndexFromNodeIndex(currentNodeIndex, targetNodeIndices, DM.PATH);

		if (game.getGhostLairTime(GHOST.BLINKY) == 0 && activePowerPills.length > 0) {
			GameView.addPoints(game, Color.RED,
					game.getAStarPath(game.getGhostCurrentNodeIndex(GHOST.BLINKY), activePowerPills[0], MOVE.NEUTRAL));
			GameView.addPoints(game, Color.YELLOW, game.getAStarPath(game.getGhostCurrentNodeIndex(GHOST.BLINKY),
					activePowerPills[0], game.getGhostLastMoveMade(GHOST.BLINKY)));
		}

		return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(), nearest, DM.PATH);
	}
}