package concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestExceptionHandlingForCompletableFutures {

	@Test
	public void processAllTasksWithHandle() {
		List<CompletableFuture<Integer>> futures = new ArrayList<>();
		List<Integer> successes = new ArrayList<>();
		List<Integer> failures = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			int record = i;
			CompletableFuture<Integer> integerCompletableFuture = performTask(i).handleAsync((integer, throwable) -> {
				log.info("1 check threading for {}", integer);
				if (throwable != null) {
					failures.add(record);
				} else {
					successes.add(record);
				}
				return integer;
			}).thenApply(integer -> {
				log.info("2 check threading for {}", integer);
				return 10;
			});

			futures.add(integerCompletableFuture);
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		log.info("successes: {}", successes);
		log.info("failures: {}", failures);
	}

	@Test
	public void testLazyWithSupplyAysnc() throws InterruptedException {
		List<CompletableFuture<Integer>> futures = new ArrayList<>();
		List<Integer> successes = new ArrayList<>();
		List<Integer> failures = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			int record = i;
			CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> performTask(record))
					.handleAsync((integer,
							throwable) -> {
						log.info("1 check threading for {}", integer);
						if (throwable != null) {
							failures.add(record);
						} else {
							successes.add(record);
						}
						return integer;
					}).thenApply(integer -> {
						log.info("2 check threading for {}", integer);
						return 10;
					});

			futures.add(integerCompletableFuture);
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		log.info("successes: {}", successes);
		log.info("failures: {}", failures);
	}

	@Test(expected = Exception.class)
	public void processAllTasksWithWhenComplete() {
		List<CompletableFuture<Integer>> futures = new ArrayList<>();
		List<Integer> successes = new ArrayList<>();
		List<Integer> failures = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			int record = i;
			CompletableFuture<Integer> integerCompletableFuture = performTask(i).whenComplete(((integer, throwable) -> {
				if (throwable != null) {
					failures.add(record);
				} else {
					successes.add(record);
				}
			}));

			futures.add(integerCompletableFuture);
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		log.info("successes: {}", successes);
		log.info("failures: {}", failures);
	}

	public void test() {

	}

	private CompletableFuture<Integer> performTask(int i) {
		try {
			log.info("Performing a task {}", i);
			Thread.sleep(100);
			if (i == 2) {
				throw new Exception("We are devil");
			}
			if (i == 8) {
				throw new Exception("We met worse");
			}
			return CompletableFuture.completedFuture(i);

		}
		catch (Exception e) {
			log.error("We have exception", e);
			CompletableFuture<Integer> f = new CompletableFuture<>();
			f.completeExceptionally(e);
			return f;
		}
	}
}