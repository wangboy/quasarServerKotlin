package com.wb.quasar;


import co.paralleluniverse.actors.*;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ActorTest {
	public static void main(String[] args) throws Exception {
		run();
	}

	public static void run() {
		new NaiveActor("naive").spawn();
		try {
			Strand.sleep(Long.MAX_VALUE);
		} catch (SuspendExecution | InterruptedException suspendExecution) {
			suspendExecution.printStackTrace();
		}
	}

	@Upgrade
	static class BadActor extends BasicActor<String, Void> {
		private int count;

		@Override
		protected Void doRun() throws InterruptedException, SuspendExecution {
			System.out.println("(re)starting actor");
			for (; ; ) {
				String m = receive(300, TimeUnit.MILLISECONDS);
				if (m != null)
					System.out.println("Got a message: " + m);
				System.out.println("I am but a lowly actor that sometimes fails: - " + (count++));

				if (ThreadLocalRandom.current().nextInt(30) == 0)
					throw new RuntimeException("darn");

				checkCodeSwap(); // this is a convenient time for a code swap
			}
		}
	}

	static class NaiveActor extends BasicActor<Void, Void> {
		private ActorRef<String> myBadActor;

		public NaiveActor(String name) {
			super(name);
		}

		@Override
		protected Void doRun() throws InterruptedException, SuspendExecution {
			spawnBadActor();

			int count = 0;
			for (; ; ) {
				receive(500, TimeUnit.MILLISECONDS);
				myBadActor.send("hi from " + self() + " number " + (count++));
			}
		}

		private void spawnBadActor() {
			myBadActor = new BadActor().spawn();
			watch(myBadActor);
		}

		@Override
		protected Void handleLifecycleMessage(LifecycleMessage m) {
			if (m instanceof ExitMessage && Objects.equals(((ExitMessage) m).getActor(), myBadActor)) {
				System.out.println("My bad actor has just died of '" + ((ExitMessage) m).getCause() + "'. Restarting.");
				spawnBadActor();
			}
			return super.handleLifecycleMessage(m);
		}
	}
}