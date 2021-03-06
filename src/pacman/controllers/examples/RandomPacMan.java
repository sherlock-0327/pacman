package pacman.controllers.examples;

import java.util.Random;
import pacman.game.Game;
import pacman.game.Constants.MOVE;
import pacman.controllers.Controller;

public final class RandomPacMan extends Controller<MOVE> {
	private Random rnd = new Random();
	private MOVE[] allMoves = MOVE.values();

	public MOVE getMove(Game game, long timeDue) {
		return allMoves[rnd.nextInt(allMoves.length)];
	}
}