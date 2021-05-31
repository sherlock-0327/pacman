package pacman.controllers.examples;

import java.util.Random;
import pacman.controllers.Controller;
import pacman.game.Game;

import static pacman.game.Constants.*;

public final class RandomNonRevPacMan extends Controller<MOVE> {
	Random rnd = new Random();

	public MOVE getMove(Game game, long timeDue) {
		MOVE[] possibleMoves = game.getPossibleMoves(game.getPacmanCurrentNodeIndex(), game.getPacmanLastMoveMade());

		return possibleMoves[rnd.nextInt(possibleMoves.length)];
	}
}