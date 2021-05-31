package pacman.controllers;

import pacman.game.Game;

public abstract class Controller<T> implements Runnable {
	private boolean alive, wasSignalled, hasComputed;
	private volatile boolean threadStillRunning;
	private long timeDue;
	private Game game;
	protected T lastMove;

	public Controller() {
		alive = true;
		wasSignalled = false;
		hasComputed = false;
		threadStillRunning = false;
	}

	public final void terminate() {
		alive = false;
		wasSignalled = true;

		synchronized (this) {
			notify();
		}
	}

	public final void update(Game game, long timeDue) {
		synchronized (this) {
			this.game = game;
			this.timeDue = timeDue;
			wasSignalled = true;
			hasComputed = false;
			notify();
		}
	}

	public final T getMove() {
		return lastMove;
	}

	public final void run() {
		while (alive) {
			synchronized (this) {
				while (!wasSignalled) {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				if (!threadStillRunning) {
					new Thread() {
						public void run() {
							threadStillRunning = true;
							lastMove = getMove(game, timeDue);
							hasComputed = true;
							threadStillRunning = false;
						}
					}.start();
				}

				wasSignalled = false;
			}
		}
	}

	public final boolean hasComputed() {
		return hasComputed;
	}

	public abstract T getMove(Game game, long timeDue);
}