package concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CompletableFutureConcurrency {
	private final static ThreadPoolExecutor threadpool = (ThreadPoolExecutor) Executors.newFixedThreadPool(5,
			new ThreadFactoryBuilder().setNameFormat("call-1-%d").build());

	public static void main(String[] args) {
		try {

			List<CompletableFuture<Integer>> futures = new ArrayList<>();
			List<Integer> successes = new ArrayList<>();
			List<Integer> failures = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				int record = i;
				CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
					try {
						return performTask(record);
					}
					catch (Exception e) {
						throw new CompletionException(e);
					}
				}, threadpool)
						.handle((integer, throwable) -> {
							if (throwable != null) {
								failures.add(record);
							} else {
								successes.add(record);
							}
							return integer;
						});

				futures.add(integerCompletableFuture);
			}
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
			log.info("successes: {}", successes);
			log.info("failures: {}", failures);
		}
		catch (Exception e) {
			log.error("hmmm, something wrong", e);
		}

	}

	public static Integer performTask(int i) throws IllegalStateException, InterruptedException {

		log.info("we are doing {}", i);
		Thread.sleep(100);
		if (i == 2) {
			throw new IllegalStateException("We're devil");
		}
		if (i == 8) {
			throw new IllegalStateException("We're worse");
		}

		return i;

	}
}
