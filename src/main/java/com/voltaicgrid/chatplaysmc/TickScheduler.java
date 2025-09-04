package com.voltaicgrid.chatplaysmc;

import net.minecraft.server.MinecraftServer;

public final class TickScheduler {
	  private static final class Task {
	    long runAt;        // absolute tick
	    final int interval; // 0 = one-shot, >0 = repeat every N ticks
	    final Runnable action;
	    Task(long runAt, int interval, Runnable action) {
	      this.runAt = runAt; this.interval = interval; this.action = action;
	    }
	  }

	  private final java.util.PriorityQueue<Task> queue =
	      new java.util.PriorityQueue<>(java.util.Comparator.comparingLong(t -> t.runAt));
	  private long currentTick = 0;

	  /** Called from END_SERVER_TICK */
	  public void tick() {
	    currentTick++;
	    while (true) {
	      Task t;
	      synchronized (queue) {
	        t = queue.peek();
	        if (t == null || t.runAt > currentTick) break;
	        queue.poll();
	      }
	      // We are already on the server thread inside the tick callback.
	      t.action.run();
	      if (t.interval > 0) {
	        t.runAt = currentTick + t.interval;
	        synchronized (queue) { queue.add(t); }
	      }
	    }
	  }

	  /** Schedule a one-shot task delayTicks from now (server thread or not). */
	  public void runLater(MinecraftServer server, int delayTicks, Runnable action) {
	    long when = Math.max(0, delayTicks);
	    server.execute(() -> { // ensure queue mutation happens on server thread
	      synchronized (queue) { queue.add(new Task(currentTick + when, 0, action)); }
	    });
	  }

	  /** Schedule a repeating task (first run after initialDelay, then every interval). */
	  public void runRepeating(MinecraftServer server, int initialDelay, int interval, Runnable action) {
	    long when = Math.max(0, initialDelay);
	    int every = Math.max(1, interval);
	    server.execute(() -> {
	      synchronized (queue) { queue.add(new Task(currentTick + when, every, action)); }
	    });
	  }
	}
