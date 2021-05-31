package pacman;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Random;

import pacman.controllers.Controller;
import pacman.controllers.HumanController;
import pacman.controllers.KeyBoardInput;
import pacman.controllers.examples.AggressiveGhosts;
import pacman.controllers.examples.Legacy;
import pacman.controllers.examples.Legacy2TheReckoning;
import pacman.controllers.examples.NearestPillPacMan;
import pacman.controllers.examples.NearestPillPacManVS;
import pacman.controllers.examples.RandomGhosts;
import pacman.controllers.examples.RandomNonRevPacMan;
import pacman.controllers.examples.RandomPacMan;
import pacman.controllers.examples.StarterGhosts;
import pacman.controllers.examples.StarterPacMan;
import pacman.game.Game;
import pacman.game.GameView;
import static pacman.game.Constants.*;

@SuppressWarnings("unused")
public class Executor {

	public static void main(String[] args) {
		int delay = 10;
		boolean visual = true;
		int numTrials = 20;

		Executor exec = new Executor();

		/* run a game in synchronous mode: game waits until controllers respond. */
		System.out.println("STARTER PACMAN vs LEGACY2THERECKONING");
		exec.runGame(new StarterPacMan(), new Legacy2TheReckoning(), visual,delay);

		/* run multiple games in batch mode - good for testing. */

//		System.out.println("STARTER PACMAN vs LEGACY2THERECONING");
//		exec.runExperiment(new StarterPacMan(), new Legacy2TheReckoning(),numTrials);
//		System.out.println("RANDOM PACMAN vs LEGACY2THERECONING");
//		exec.runExperiment(new RandomPacMan(), new Legacy2TheReckoning(),numTrials);
//		System.out.println("NEAREST PILL PACMAN vs LEGACY2THERECONING");
//		exec.runExperiment(new NearestPillPacMan(), new Legacy2TheReckoning(),numTrials);
//		
//		
//		System.out.println("STARTER PACMAN vs starter GHOSTS");
//		exec.runExperiment(new StarterPacMan(), new StarterGhosts(),numTrials);
//		System.out.println("RANDOM PACMAN vs RANDOM GHOSTS");
//		exec.runExperiment(new RandomPacMan(),  new StarterGhosts(),numTrials);
//		System.out.println("NEAREST PILL PACMAN vs RANDOM GHOSTS");
//		exec.runExperiment(new NearestPillPacMan(), new StarterGhosts(),numTrials);

		/* run the game in asynchronous mode. */

//		exec.runGameTimed(new MyPacMan(),new AggressiveGhosts(),visual);
//		exec.runGameTimed(new RandomPacMan(), new AvengersEvolution(evolutionFile),visual);
//		exec.runGameTimed(new HumanController(new KeyBoardInput()),new StarterGhosts(),visual);	

		/*
		 * run the game in asynchronous mode but advance as soon as both controllers are
		 * ready - this is the mode of the competition. time limit of DELAY ms still
		 * applies.
		 */

//		boolean visual=true;
//		boolean fixedTime=false;
//		exec.runGameTimedSpeedOptimised(new MyMCTSPacMan(new AggressiveGhosts()),new AggressiveGhosts(),fixedTime,visual);

		/*
		 * run game in asynchronous mode and record it to file for replay at a later
		 * stage.
		 */

//		String fileName = "replay.txt";
//		exec.runGameTimedRecorded(new HumanController(new KeyBoardInput()), new RandomGhosts(), visual, fileName);
//		exec.replayGame(fileName, visual);
	}

	public void runExperiment(Controller<MOVE> pacManController, Controller<EnumMap<GHOST, MOVE>> ghostController,
			int trials) {
		double avgScore = 0;

		Random rnd = new Random(0);
		Game game;

		for (int i = 0; i < trials; i++) {
			game = new Game(rnd.nextLong());

			while (!game.gameOver()) {
				game.advanceGame(pacManController.getMove(game.copy(), System.currentTimeMillis() + DELAY),
						ghostController.getMove(game.copy(), System.currentTimeMillis() + DELAY));
			}

			avgScore += game.getScore();
			System.out.println(i + "\t" + game.getScore());
		}

		System.out.println(avgScore / trials);
	}

	public void runGame(Controller<MOVE> pacManController, Controller<EnumMap<GHOST, MOVE>> ghostController,
			boolean visual, int delay) {
		Game game = new Game(0);

		GameView gv = null;

		if (visual)
			gv = new GameView(game).showGame();

		while (!game.gameOver()) {
			game.advanceGame(pacManController.getMove(game.copy(), -1), ghostController.getMove(game.copy(), -1));

			try {
				Thread.sleep(delay);
			} catch (Exception e) {
			}

			if (visual)
				gv.repaint();
		}
	}

	public void runGameTimed(Controller<MOVE> pacManController, Controller<EnumMap<GHOST, MOVE>> ghostController,
			boolean visual) {
		Game game = new Game(0);

		GameView gv = null;

		if (visual)
			gv = new GameView(game).showGame();

		if (pacManController instanceof HumanController)
			gv.getFrame().addKeyListener(((HumanController) pacManController).getKeyboardInput());

		new Thread(pacManController).start();
		new Thread(ghostController).start();

		while (!game.gameOver()) {
			pacManController.update(game.copy(), System.currentTimeMillis() + DELAY);
			ghostController.update(game.copy(), System.currentTimeMillis() + DELAY);

			try {
				Thread.sleep(DELAY);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			game.advanceGame(pacManController.getMove(), ghostController.getMove());

			if (visual)
				gv.repaint();
		}

		pacManController.terminate();
		ghostController.terminate();
	}

	public void runGameTimedSpeedOptimised(Controller<MOVE> pacManController,
			Controller<EnumMap<GHOST, MOVE>> ghostController, boolean fixedTime, boolean visual) {
		Game game = new Game(0);

		GameView gv = null;

		if (visual)
			gv = new GameView(game).showGame();

		if (pacManController instanceof HumanController)
			gv.getFrame().addKeyListener(((HumanController) pacManController).getKeyboardInput());

		new Thread(pacManController).start();
		new Thread(ghostController).start();

		while (!game.gameOver()) {
			pacManController.update(game.copy(), System.currentTimeMillis() + DELAY);
			ghostController.update(game.copy(), System.currentTimeMillis() + DELAY);

			try {
				int waited = DELAY / INTERVAL_WAIT;

				for (int j = 0; j < DELAY / INTERVAL_WAIT; j++) {
					Thread.sleep(INTERVAL_WAIT);

					if (pacManController.hasComputed() && ghostController.hasComputed()) {
						waited = j;
						break;
					}
				}

				if (fixedTime)
					Thread.sleep(((DELAY / INTERVAL_WAIT) - waited) * INTERVAL_WAIT);

				game.advanceGame(pacManController.getMove(), ghostController.getMove());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (visual)
				gv.repaint();
		}

		pacManController.terminate();
		ghostController.terminate();
	}

	public void runGameTimedRecorded(Controller<MOVE> pacManController,
			Controller<EnumMap<GHOST, MOVE>> ghostController, boolean visual, String fileName) {
		StringBuilder replay = new StringBuilder();

		Game game = new Game(0);

		GameView gv = null;

		if (visual) {
			gv = new GameView(game).showGame();

			if (pacManController instanceof HumanController)
				gv.getFrame().addKeyListener(((HumanController) pacManController).getKeyboardInput());
		}

		new Thread(pacManController).start();
		new Thread(ghostController).start();

		while (!game.gameOver()) {
			pacManController.update(game.copy(), System.currentTimeMillis() + DELAY);
			ghostController.update(game.copy(), System.currentTimeMillis() + DELAY);

			try {
				Thread.sleep(DELAY);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			game.advanceGame(pacManController.getMove(), ghostController.getMove());

			if (visual)
				gv.repaint();

			replay.append(game.getGameState() + "\n");
		}

		pacManController.terminate();
		ghostController.terminate();

		saveToFile(replay.toString(), fileName, false);
	}

	public void replayGame(String fileName, boolean visual) {
		ArrayList<String> timeSteps = loadReplay(fileName);

		Game game = new Game(0);

		GameView gv = null;

		if (visual)
			gv = new GameView(game).showGame();

		for (int j = 0; j < timeSteps.size(); j++) {
			game.setGameState(timeSteps.get(j));

			try {
				Thread.sleep(DELAY);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (visual)
				gv.repaint();
		}
	}

	// save file for replays
	public static void saveToFile(String data, String name, boolean append) {
		try {
			FileOutputStream outS = new FileOutputStream(name, append);
			PrintWriter pw = new PrintWriter(outS);

			pw.println(data);
			pw.flush();
			outS.close();

		} catch (IOException e) {
			System.out.println("Could not save data!");
		}
	}

	// load a replay
	private static ArrayList<String> loadReplay(String fileName) {
		ArrayList<String> replay = new ArrayList<String>();

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
			String input = br.readLine();

			while (input != null) {
				if (!input.equals(""))
					replay.add(input);

				input = br.readLine();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return replay;
	}
}