import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CompletableFutureConcurrency {

	public static void main(String[] args) {
		try {

			List<CompletableFuture<Integer>> futures = new ArrayList<>();
			List<Integer> successes = new ArrayList<>();
			List<Integer> failures = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				int record = i;
				CompletableFuture<Integer> integerCompletableFuture = performTask(i)
						.whenComplete(((integer, throwable) -> {
							if (throwable != null) {
								failures.add(record);
							} else {
								successes.add(record);
							}
							//return integer;
						}));

				futures.add(integerCompletableFuture);
			}
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
			log.info("successes: {}", successes);
			log.info("failures: {}", failures);
		}catch (Exception e) {
			log.error("hmmm, something wrong", e);
		}

	}

	public static CompletableFuture<Integer> performTask(int i){
		try{
			log.info("we are doing {}", i);
			Thread.sleep(100);
			if (i == 2) throw new Exception("We're devil");
			if (i == 8) throw new Exception("We're worse");
		// generate random number between 0 to 10 and divide I+10 by that to
			// fail
			// some
			// tasks
			// randomly.
			return CompletableFuture.completedFuture(i);
		}catch(Exception e){
			log.error("exception for {}", i, e);
			CompletableFuture<Integer> f = new CompletableFuture<>();
			f.completeExceptionally(e);
			return f;
		}

	}
}
