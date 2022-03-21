package concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestSimpleExample {
	private final ThreadPoolExecutor threadpool1 = (ThreadPoolExecutor) Executors.newFixedThreadPool(5,
			new ThreadFactoryBuilder().setNameFormat("call-1-%d").build());
	private final ThreadPoolExecutor threadpool2 = (ThreadPoolExecutor) Executors.newFixedThreadPool(5,
			new ThreadFactoryBuilder().setNameFormat("call-2-%d").build());

	private final ThreadPoolExecutor threadPoolProcess = (ThreadPoolExecutor) Executors.newFixedThreadPool(5,
			new ThreadFactoryBuilder().setNameFormat("process-%d").build());
	private final ThreadPoolExecutor threadpoolForRes = (ThreadPoolExecutor) Executors.newFixedThreadPool(5,
			new ThreadFactoryBuilder().setNameFormat("result-%d").build());

	private List<Integer> dataSource = getDataSource();

	private List<Integer> getDataSource() {
		List<Integer> dataSource = new ArrayList<>();
		for (int c = 0; c < 10; c++) {
			dataSource.add(c);
		}
		return dataSource;
	}

	{
		String log4jConfPath = "log4j.properties";
		PropertyConfigurator.configure(log4jConfPath);

	}

	@Test
	public void test_RxjavaChained() throws InterruptedException {
		CountDownLatch doneSignal = new CountDownLatch(1);
		long start = System.currentTimeMillis();

		Flowable.fromIterable(dataSource)
				.doOnNext(integer -> {
					log.info("pool queue size, call-1 : {}, call-2: {}, process- {}, res- {}", threadpool1.getQueue().size(),
							threadpool2.getQueue().size(), threadPoolProcess.getQueue().size(), threadpoolForRes.getQueue().size());
				})
				.map(v -> CompletableFuture.supplyAsync(() -> firstCall(v), threadpool1).thenApplyAsync(this::secondCall, threadpool2))
				.flatMap(cf -> Flowable.fromFuture(cf).subscribeOn(Schedulers.from(threadPoolProcess)))
				.subscribe(v -> log.info("we get {}", v),
						e -> log.error("error", e),
						() -> {
							doneSignal.countDown();
							log.info("Running time is {} ms", (System.currentTimeMillis() - start));
						});

		doneSignal.await();
	}

	@Test
	public void test_Rxjava() throws InterruptedException {
		CountDownLatch doneSignal = new CountDownLatch(1);

		long start = System.currentTimeMillis();

		Flowable.fromIterable(dataSource)
				.doOnNext(integer -> {
					log.info("pool queue size, call-1 : {}, call-2: {}, process- {}, res- {}", threadpool1.getQueue().size(),
							threadpool2.getQueue().size(), threadPoolProcess.getQueue().size(), threadpoolForRes.getQueue().size());
				})
				.map(v -> CompletableFuture.supplyAsync(() -> firstCall(v), threadpool1))
				.flatMap(cf -> Flowable.fromFuture(cf).subscribeOn(Schedulers.from(threadPoolProcess)))
				.map(v -> CompletableFuture.supplyAsync(() -> secondCall(v), threadpool2))
				.flatMap(cf -> Flowable.fromFuture(cf).subscribeOn(Schedulers.from(threadPoolProcess)))
				.subscribe(v -> log.info("we get {}", v),
						e -> log.error("error", e),
						() -> {
							doneSignal.countDown();
							log.info("Running time is {} ms", (System.currentTimeMillis() - start));
						});
		doneSignal.await();

	}

	@Test
	public void test_FastWithCollectorsList() {
		long start = System.currentTimeMillis();
		List<CompletableFuture<Integer>> futures = new ArrayList<>();

		dataSource.stream()
				.map(volume -> {
					log.info("pool queue size, call-1 : {}, call-2: {}, process- {}, res- {}", threadpool1.getQueue().size(),
							threadpool2.getQueue().size(), threadPoolProcess.getQueue().size(), threadpoolForRes.getQueue().size());
					CompletableFuture<Integer> resultOfFirstCall = CompletableFuture.supplyAsync(() -> firstCall(volume), threadpool1);
					CompletableFuture<Integer> resultOfSecondCall = resultOfFirstCall.thenApplyAsync(this::secondCall, threadpool2);
					futures.add(resultOfSecondCall);
					return resultOfSecondCall;
				})
				.collect(Collectors.toList())

				.forEach(cf -> {
					Integer volume = cf.join();
					log.info("Get value for cf {}", volume);

				});
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

		log.info("Running time is {} ms", (System.currentTimeMillis() - start));
	}

	@Test
	public void test_FastWithAsyncJoin() {
		long start = System.currentTimeMillis();
		List<CompletableFuture<Integer>> futures = new ArrayList<>();
		dataSource.stream()
				.map(volume -> {
					log.info("pool queue size, call-1 : {}, call-2: {}, process- {}, res- {}", threadpool1.getQueue().size(),
							threadpool2.getQueue().size(), threadPoolProcess.getQueue().size(), threadpoolForRes.getQueue().size());
					CompletableFuture<Integer> resultOfFirstCall = CompletableFuture.supplyAsync(() -> firstCall(volume), threadpool1);
					CompletableFuture<Integer> resultOfSecondCall = resultOfFirstCall.thenApplyAsync(this::secondCall, threadpool2);
					futures.add(resultOfSecondCall);
					return resultOfSecondCall;
				})
				.forEach(cf -> CompletableFuture.supplyAsync(cf::join, threadpoolForRes).thenAccept(v -> log.info("Get value for cf {}", v)));
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

		log.info("Running time is {} ms", (System.currentTimeMillis() - start));
	}

	@Test
	@Ignore
	public void test_Slow() {
		long start = System.currentTimeMillis();
		List<CompletableFuture<Integer>> futures = new ArrayList<>();
		dataSource.stream()
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
					log.info("Joined task {}", volume);

				});
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

		log.info("Running time is {} ms", (System.currentTimeMillis() - start));
	}

	public Integer firstCall(Integer value) {
		log.info("Calling first API {} ", value);
		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException e) {
			log.error("Error", e);
		}
		return value;
	}

	public Integer secondCall(Integer value) {
		log.info("Calling second API {} ", value);
		try {
			Thread.sleep(2000);
		}
		catch (InterruptedException e) {
			log.error("Error", e);
		}
		return value * 10;
	}
}