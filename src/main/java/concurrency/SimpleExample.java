package concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleExample {

	private final ExecutorService threadpool1 = Executors.newFixedThreadPool(1,
			new ThreadFactoryBuilder().setNameFormat("call-1").build());
	private final ExecutorService threadpool2 = Executors.newFixedThreadPool(1,
			new ThreadFactoryBuilder().setNameFormat("call-2").build());
	private final ExecutorService threadpoolForRes = Executors.newFixedThreadPool(1,
			new ThreadFactoryBuilder().setNameFormat("result").build());

	public SimpleExample() {

	}

	public static void main(String[] args) {
		List<Integer> holesToDig = new ArrayList<>();
		Random random = new Random();
		for (int c = 0; c < 100; c++) {
			holesToDig.add(c);
		}
		new SimpleExample().execute(holesToDig);
	}

	public void execute(List<Integer> holeVolumes) {
		long start = System.currentTimeMillis();
		List<CompletableFuture<Integer>> futures = new ArrayList<>();
		holeVolumes.stream()
				.map(volume -> {
					log.info("Prepare completable futures");
					CompletableFuture<Integer> resultOfFirstCall = CompletableFuture.supplyAsync(() -> firstCall(volume), threadpool1);
					CompletableFuture<Integer> resultOfSecondCall = resultOfFirstCall.thenApplyAsync(this::secondCall, threadpool2);
					futures.add(resultOfSecondCall);
					return resultOfSecondCall;
				})
				//.collect(Collectors.toList())

				.forEach(cf -> {
							log.info("wait completable future in another thread");
							CompletableFuture.supplyAsync(cf::join, threadpoolForRes)
									.thenAccept(v -> System.out.println("Dug a hole and filled it back in.  Net volume: " + v));
						}
						//Integer volume = cf.join();

				);
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

		log.info("Dug up and filled back in " + holeVolumes.size() + " holes in " + (System.currentTimeMillis() - start) + " ms");
	}

	public void executeSlow(List<Integer> holeVolumes) {
		long start = System.currentTimeMillis();
		List<CompletableFuture<Integer>> futures = new ArrayList<>();
		holeVolumes.stream()
				.map(volume -> {
					log.info("Prepare completable futures");
					CompletableFuture<Integer> resultOfFirstCall = CompletableFuture.supplyAsync(() -> firstCall(volume), threadpool1);
					CompletableFuture<Integer> resultOfSecondCall = resultOfFirstCall.thenApplyAsync(this::secondCall, threadpool2);
					futures.add(resultOfSecondCall);
					return resultOfSecondCall;
				})
				//.collect(Collectors.toList())

				.forEach(cf -> {
					log.info("wait completable future in another thread");
					Integer volume = cf.join();
					System.out.println("Dug a hole and filled it back in.  Net volume: " + volume);

				});
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

		log.info("Dug up and filled back in " + holeVolumes.size() + " holes in " + (System.currentTimeMillis() - start) + " ms");
	}

	public Integer firstCall(Integer value) {
		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException e) {
		}
		log.info("Calling first API {} ", value);
		return value;
	}

	public Integer secondCall(Integer value) {
		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException e) {
		}
		log.info("Calling second API {} ", value);
		return value * 10;
	}
}
