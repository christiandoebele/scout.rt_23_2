/*
 * Copyright (c) 2010, 2024 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.platform.cache;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.context.RunContexts;
import org.eclipse.scout.rt.platform.exception.ProcessingException;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.Jobs;
import org.eclipse.scout.rt.platform.transaction.AbstractTransactionMember;
import org.eclipse.scout.rt.platform.transaction.ITransaction;
import org.eclipse.scout.rt.platform.util.CollectionUtility;
import org.eclipse.scout.rt.platform.util.collection.AbstractTransactionalMap.AbstractMapTransactionMember;
import org.eclipse.scout.rt.platform.util.concurrent.FutureCancelledError;
import org.junit.ComparisonFailure;
import org.junit.Test;

/**
 * @since 5.2
 */
public class BasicCacheTest {

  protected ICache<Integer, String> createCache(String id) {
    return createCache(id, new ICacheValueResolver<>() {
      private int m_counter;

      @Override
      public String resolve(Integer key) {
        if (13 == key) {
          return null;
        }
        if (1337 == key) {
          throw new ProcessingException("Test exception - thrown");
        }
        m_counter++;
        return key + "." + m_counter;
      }
    });
  }

  protected ICache<Integer, String> createCache(String id, ICacheValueResolver<Integer, String> resolver) {
    @SuppressWarnings("unchecked")
    ICacheBuilder<Integer, String> cacheBuilder = BEANS.get(ICacheBuilder.class);
    ICache<Integer, String> cache = cacheBuilder
        .withCacheId(id)
        .withValueResolver(resolver)
        .withThreadSafe(false)
        .withReplaceIfExists(true)
        .build();
    cache.invalidate(new AllCacheEntryFilter<>(), true);
    return cache;
  }

  @Test
  public void testCacheBasic() {
    ICache<Integer, String> cache = createCache("BasicCacheTestCacheId_testCacheBasic");

    // test get
    assertEquals("2.1", cache.get(2));
    assertEquals("2.1", cache.get(2));
    assertEquals("2.1", cache.get(2));
    assertEquals("5.2", cache.get(5));

    // test get all
    Map<Integer, String> resultMap = cache.getAll(CollectionUtility.arrayList(2, 7, 8, 9));
    assertEquals(4, resultMap.size());
    assertEquals("2.1", resultMap.get(2));
    assertEquals("2.1", cache.get(2));
    assertEquals("7.3", resultMap.get(7));
    assertEquals("7.3", cache.get(7));
    assertEquals("8.4", resultMap.get(8));
    assertEquals("8.4", cache.get(8));
    assertEquals("9.5", resultMap.get(9));
    assertEquals("9.5", cache.get(9));

    assertEquals(5, cache.getUnmodifiableMap().size());

    // test invalidate
    cache.invalidate(new KeyCacheEntryFilter<>(CollectionUtility.arrayList(5, 8)), true);
    assertEquals("5.6", cache.get(5));
    assertEquals("8.7", cache.get(8));

    assertEquals(5, cache.getUnmodifiableMap().size());

    // test invalidate all
    cache.invalidate(new AllCacheEntryFilter<>(), true);
    assertEquals(0, cache.getUnmodifiableMap().size());
  }

  @Test
  public void testCacheNullValues() {
    ICache<Integer, String> cache = createCache("BasicCacheTestCacheId_testCacheNullValues");

    assertEquals("2.1", cache.get(2));
    assertNull(cache.get(null));

    // unresolvable keys
    assertNull(cache.get(13));

    assertEquals(1, cache.getUnmodifiableMap().size());

    Map<Integer, String> resultMap = cache.getAll(CollectionUtility.arrayList(3, null));
    assertEquals(1, resultMap.size());
    assertEquals("3.2", resultMap.get(3));

    assertEquals(2, cache.getUnmodifiableMap().size());

    cache.invalidate(new KeyCacheEntryFilter<>(CollectionUtility.arrayList(2, null)), true);
    assertEquals(1, cache.getUnmodifiableMap().size());
    assertEquals("3.2", resultMap.get(3));

    Map<Integer, String> emptyResultMap = cache.getAll(null);
    assertEquals(0, emptyResultMap.size());
    cache.invalidate(null, true);
  }

  @Test(expected = ProcessingException.class)
  public void testCacheExceptionDuringResolve() {
    ICache<Integer, String> cache = createCache("BasicCacheTestCacheId_testCacheExceptionDuringCreation");
    cache.get(1337);
  }

  @Test
  public void testCacheTransactional() {
    testCacheTransactional(true);
    testCacheTransactional(false);
  }

  protected void testCacheTransactional(boolean transactionalFastForward) {
    @SuppressWarnings("unchecked")
    ICache<Integer, String> cache = BEANS.get(ICacheBuilder.class)
        .withCacheId("BasicCacheTestCacheId#testInvalidateDuringResolve")
        .withValueResolver((ICacheValueResolver<Integer, String>) key -> "value_" + key)
        .withReplaceIfExists(true)
        .withTransactional(true)
        .withTransactionalFastForward(transactionalFastForward)
        .build();

    assertEquals("value_1", RunContexts.empty().call(() -> cache.get(1)));

    Map<Integer, String> expectedMap = new HashMap<>();
    expectedMap.put(1, "value_1");
    expectedMap.put(2, "value_2");
    expectedMap.put(3, "value_3");
    assertEquals(expectedMap, RunContexts.empty().call(() -> cache.getAll(Arrays.asList(1, 2, 3))));

    RunContexts.empty().run(() -> cache.invalidate(new KeyCacheEntryFilter<>(Arrays.asList(1)), true));
    expectedMap.remove(1);
    assertEquals(expectedMap, RunContexts.empty().call(() -> cache.getUnmodifiableMap()));

    RunContexts.empty().run(() -> cache.invalidate(new KeyCacheEntryFilter<>(Arrays.asList(1, 2)), true));
    expectedMap.remove(2);
    assertEquals(expectedMap, RunContexts.empty().call(() -> cache.getUnmodifiableMap()));

    assertEquals("value_2", RunContexts.empty().call(() -> cache.get(2)));

    RunContexts.empty().run(() -> cache.invalidate(new AllCacheEntryFilter<>(), true));
    assertEquals(Collections.emptyMap(), RunContexts.empty().call(() -> cache.getUnmodifiableMap()));
  }

  @Test
  public void testNoFastForwardOfDirty() {
    AtomicReference<Function<Integer, String>> valueResolver = new AtomicReference<>();

    @SuppressWarnings("unchecked")
    ICache<Integer, String> cache = BEANS.get(ICacheBuilder.class)
        .withCacheId("BasicCacheTestCacheId#testNoFastForwardOfDirty")
        .withValueResolver((ICacheValueResolver<Integer, String>) key -> {
          Function<Integer, String> internalResolver = valueResolver.get();
          return internalResolver.apply(key);
        })
        .withReplaceIfExists(true)
        .withTransactional(true)
        .withTransactionalFastForward(true)
        .build();

    valueResolver.set(key -> "val_A_" + key);
    assertEquals("val_A_1", RunContexts.empty().call(() -> cache.get(1)));

    RunContexts.empty().run(() -> {
      valueResolver.set(key -> "val_AA_" + key); // change value "in local transaction"
      cache.invalidate(new AllCacheEntryFilter<>(), true);
      assertEquals("val_AA_1", cache.get(1));
    });

    assertEquals("val_AA_1", RunContexts.empty().call(() -> cache.get(1)));
    // remove all cache values -> fast-forward in local transaction
    RunContexts.empty().run(() -> cache.invalidate(new AllCacheEntryFilter<>(), true));

    RunContexts.empty().run(() -> {
      valueResolver.set(key -> "val_B_" + key); // change value "in local transaction"
      cache.invalidate(new AllCacheEntryFilter<>(), true);
      assertEquals("val_B_1", cache.get(1));
    });

    assertEquals("val_B_1", RunContexts.empty().call(() -> cache.getCachedValue(1))); // fast-forward success
    assertEquals("val_B_1", RunContexts.empty().call(() -> cache.get(1))); // see new value

    try {
      RunContexts.empty().run(() -> {
        Function<Integer, String> oldResolver = valueResolver.getAndSet(key -> "val_C_" + key); // change value "in local transaction"
        cache.invalidate(new AllCacheEntryFilter<>(), true);
        assertEquals("val_C_1", cache.get(1));

        // transaction rollback
        valueResolver.set(oldResolver); // change value back
        throw new ProcessingException("rollback");
      });
    }
    catch (ProcessingException e) {
      // nop
    }

    assertEquals("val_B_1", RunContexts.empty().call(() -> cache.get(1)));

    // remove all cache values -> fast-forward in local, rolled back transaction
    RunContexts.empty().run(() -> cache.invalidate(new AllCacheEntryFilter<>(), true));

    try {
      RunContexts.empty().run(() -> {
        Function<Integer, String> oldResolver = valueResolver.getAndSet(key -> "val_CC_" + key); // change value "in local transaction"
        cache.invalidate(new AllCacheEntryFilter<>(), true);
        assertEquals("val_CC_1", cache.get(1));

        // transaction rollback
        valueResolver.set(oldResolver); // change value back
        throw new ProcessingException("rollback");
      });
    }
    catch (ProcessingException e) {
      // nop
    }

    assertTrue(RunContexts.empty().call(() -> cache.getUnmodifiableMap().isEmpty()));
    assertEquals("val_B_1", RunContexts.empty().call(() -> cache.get(1)));
  }

  /**
   * A transaction only reading values (does not change data in source and therefore does not call 'invalidate') should
   * see current values.
   */
  @Test
  public void testReadingOnlyTransactionSeesCurrentValue() {
    AtomicReference<Function<Integer, String>> valueResolver = new AtomicReference<>();

    @SuppressWarnings("unchecked")
    ICache<Integer, String> cache = BEANS.get(ICacheBuilder.class)
        .withCacheId("BasicCacheTestCacheId#testReadingOnlyTransactionSeesCurrentValue")
        .withValueResolver((ICacheValueResolver<Integer, String>) key -> {
          Function<Integer, String> internalResolver = valueResolver.get();
          return internalResolver.apply(key);
        })
        .withReplaceIfExists(true)
        .withTransactional(true)
        .withTransactionalFastForward(true)
        .build();

    valueResolver.set(key -> "val_A_" + key);
    assertEquals("val_A_1", RunContexts.empty().call(() -> cache.get(1)));

    AtomicReference<String> valueRead1 = new AtomicReference<>();
    AtomicReference<String> valueRead2 = new AtomicReference<>();
    Phaser dataReadPhaser = new Phaser(2);
    IFuture<Void> readingTransactionFuture = Jobs.schedule(() -> {
      valueRead1.set(cache.get(1));
      valueRead2.set(cache.get(2));
      dataReadPhaser.arriveAndAwaitAdvance(); // phase A
      dataReadPhaser.arriveAndAwaitAdvance(); // phase B
      valueRead1.set(cache.get(1));
      valueRead2.set(cache.get(2));
      dataReadPhaser.arriveAndAwaitAdvance(); // phase C
    }, Jobs.newInput().withRunContext(RunContexts.empty()));

    dataReadPhaser.arriveAndAwaitAdvance(); // phase A
    assertEquals("val_A_1", valueRead1.get());
    assertEquals("val_A_2", valueRead2.get());

    RunContexts.empty().run(() -> {
      valueResolver.set(key -> "val_B_" + key); // change value "in local transaction"
      cache.invalidate(new AllCacheEntryFilter<>(), true);
      assertEquals("val_B_2", cache.get(2));
    });

    dataReadPhaser.arriveAndAwaitAdvance(); // phase B
    dataReadPhaser.arriveAndAwaitAdvance(); // phase C

    assertEquals("val_B_1", valueRead1.get());
    assertEquals("val_B_2", valueRead2.get());

    readingTransactionFuture.awaitDone();
  }

  @Test
  public void testInvalidateDuringResolve() throws InterruptedException {
    testInvalidateDuringResolve(true, false, false, false);
    testInvalidateDuringResolve(true, false, false, true);
    testInvalidateDuringResolve(false, false, false, false);
    testInvalidateDuringResolve(false, false, false, true);
  }

  @Test
  public void testInvalidateDuringResolve_secondLoad() throws InterruptedException {
    testInvalidateDuringResolve(true, true, false, false);
    testInvalidateDuringResolve(true, true, false, true);
    testInvalidateDuringResolve(false, true, false, false);
    testInvalidateDuringResolve(false, true, false, true);
  }

  @Test
  public void testInvalidateDuringResolve_allKeyInvalidate() throws InterruptedException {
    testInvalidateDuringResolve(true, false, true, false);
    testInvalidateDuringResolve(true, false, true, true);
    testInvalidateDuringResolve(false, false, true, false);
    testInvalidateDuringResolve(false, false, true, true);
  }

  @Test
  public void testInvalidateDuringResolve_secondLoad_allKeyInvalidate() throws InterruptedException {
    testInvalidateDuringResolve(true, true, true, false);
    testInvalidateDuringResolve(true, true, true, true);
    testInvalidateDuringResolve(false, true, true, false);
    testInvalidateDuringResolve(false, true, true, true);
  }

  /**
   * Tests what happens if resolve is slow and an invalidate happens during resolve
   */
  protected void testInvalidateDuringResolve(boolean transactionalFastForward, boolean secondCacheLoadAfterInvalidate, boolean allKeyInvalidate, boolean getAll) throws InterruptedException {
    AtomicReference<Function<Integer, String>> valueResolver = new AtomicReference<>();

    @SuppressWarnings("unchecked")
    ICache<Integer, String> cache = BEANS.get(ICacheBuilder.class)
        .withCacheId("BasicCacheTestCacheId#testInvalidateDuringResolve")
        .withValueResolver((ICacheValueResolver<Integer, String>) key -> {
          Function<Integer, String> internalResolver = valueResolver.get();
          return internalResolver.apply(key);
        })
        .withReplaceIfExists(true)
        .withTransactional(true)
        .withTransactionalFastForward(transactionalFastForward)
        .build();

    CountDownLatch resolvingLatch = new CountDownLatch(1);
    CountDownLatch resolvingBlockingLatch = new CountDownLatch(1);
    valueResolver.set(key -> {
      resolvingLatch.countDown();
      try {
        resolvingBlockingLatch.await();
      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return "oldValue_" + key;
    });
    IFuture<String> future = Jobs.schedule(() -> {
      if (getAll) {
        return cache.getAll(Arrays.asList(1, 3, 5)).get(1);
      }
      else {
        return cache.get(1);
      }
    }, Jobs.newInput().withRunContext(RunContexts.empty()));
    resolvingLatch.await();

    valueResolver.set(key -> "newValue_" + key); // value is "changed" in source
    // this value change will call invalidate (e.g. in a transaction value is changed on database and then 'invalidate' on cache is called)
    ICacheEntryFilter<Integer, String> invalidateEntryFilter = allKeyInvalidate ? new AllCacheEntryFilter<>() : new KeyCacheEntryFilter<>(Collections.singleton(1));
    RunContexts.empty().run(() -> cache.invalidate(invalidateEntryFilter, true));

    if (secondCacheLoadAfterInvalidate) {
      // new transaction accesses cache
      assertEquals("newValue_1", RunContexts.empty().call(() -> {
        if (getAll) {
          return cache.getAll(Arrays.asList(1, 2, 3)).get(1);
        }
        else {
          return cache.get(1);
        }
      }));
    }

    // release first transaction in job
    resolvingBlockingLatch.countDown();
    future.awaitDone();

    // access cache value again - expected to be still "newValue"
    String value2 = RunContexts.empty().call(() -> {
      if (getAll) {
        return cache.getAll(Arrays.asList(1, 4, 5)).get(1);
      }
      else {
        return cache.get(1);
      }
    });
    assertEquals("newValue_1", value2);
    assertEquals("newValue_2", RunContexts.empty().call(() -> cache.get(2)));
    assertEquals("newValue_3", RunContexts.empty().call(() -> cache.get(3)));
    assertEquals("newValue_4", RunContexts.empty().call(() -> cache.get(4)));
    assertEquals("newValue_5", RunContexts.empty().call(() -> cache.get(5)));
  }

  @Test
  public void testInvalidateBeforeAndDuringResolve() throws InterruptedException {
    testInvalidateBeforeAndDuringResolve(true, false, false, false);
    testInvalidateBeforeAndDuringResolve(true, false, false, true);
    testInvalidateBeforeAndDuringResolve(false, false, false, false);
    testInvalidateBeforeAndDuringResolve(false, false, false, true);
  }

  @Test
  public void testInvalidateBeforeAndDuringResolve_secondLoad() throws InterruptedException {
    testInvalidateBeforeAndDuringResolve(true, true, false, false);
    testInvalidateBeforeAndDuringResolve(true, true, false, true);
    testInvalidateBeforeAndDuringResolve(false, true, false, false);
    testInvalidateBeforeAndDuringResolve(false, true, false, true);
  }

  @Test
  public void testInvalidateBeforeAndDuringResolve_allKeyInvalidate() throws InterruptedException {
    testInvalidateBeforeAndDuringResolve(true, false, true, false);
    testInvalidateBeforeAndDuringResolve(true, false, true, true);
    testInvalidateBeforeAndDuringResolve(false, false, true, false);
    testInvalidateBeforeAndDuringResolve(false, false, true, true);
  }

  @Test
  public void testInvalidateBeforeAndDuringResolve_secondLoad_allKeyInvalidate() throws InterruptedException {
    testInvalidateBeforeAndDuringResolve(true, true, true, false);
    testInvalidateBeforeAndDuringResolve(true, true, true, true);
    testInvalidateBeforeAndDuringResolve(false, true, true, false);
    testInvalidateBeforeAndDuringResolve(false, true, true, true);
  }

  /**
   * Tests what happens if resolve is slow and an invalidate happens during resolve
   */
  protected void testInvalidateBeforeAndDuringResolve(boolean transactionalFastForward, boolean secondCacheLoadAfterInvalidate, boolean allKeyInvalidate, boolean getAll) throws InterruptedException {
    AtomicReference<Function<Integer, String>> valueResolver = new AtomicReference<>();

    @SuppressWarnings("unchecked")
    ICache<Integer, String> cache = BEANS.get(ICacheBuilder.class)
        .withCacheId("BasicCacheTestCacheId#testInvalidateBeforeAndDuringResolve")
        .withValueResolver((ICacheValueResolver<Integer, String>) key -> {
          Function<Integer, String> internalResolver = valueResolver.get();
          return internalResolver.apply(key);
        })
        .withReplaceIfExists(true)
        .withTransactional(true)
        .withTransactionalFastForward(transactionalFastForward)
        .build();

    CountDownLatch invalidateBlockingLatch = new CountDownLatch(1);
    ICacheEntryFilter<Integer, String> invalidateEntryFilter = allKeyInvalidate ? new AllCacheEntryFilter<>() : new KeyCacheEntryFilter<>(Collections.singleton(1));
    IFuture<Void> invalidateFuture = Jobs.schedule(() -> {
      cache.invalidate(invalidateEntryFilter, true);
      invalidateBlockingLatch.await();
    }, Jobs.newInput().withRunContext(RunContexts.empty()));

    CountDownLatch resolvingLatch = new CountDownLatch(1);
    CountDownLatch resolvingBlockingLatch = new CountDownLatch(1);
    valueResolver.set(key -> {
      resolvingLatch.countDown();
      try {
        resolvingBlockingLatch.await();
      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return "oldValue_" + key;
    });
    IFuture<String> future = Jobs.schedule(() -> {
      if (getAll) {
        return cache.getAll(Arrays.asList(1, 3, 5)).get(1);
      }
      else {
        return cache.get(1);
      }
    }, Jobs.newInput().withRunContext(RunContexts.empty()));
    resolvingLatch.await();

    valueResolver.set(key -> "newValue_" + key); // value is "changed" in source
    // invalidate by already started invalidation job
    invalidateBlockingLatch.countDown();
    invalidateFuture.awaitDone();

    if (secondCacheLoadAfterInvalidate) {
      // new transaction accesses cache
      assertEquals("newValue_1", RunContexts.empty().call(() -> {
        if (getAll) {
          return cache.getAll(Arrays.asList(1, 2, 3)).get(1);
        }
        else {
          return cache.get(1);
        }
      }));
    }

    // release first transaction in job
    resolvingBlockingLatch.countDown();
    future.awaitDone();

    // access cache value again - expected to be still "newValue"
    String value2 = RunContexts.empty().call(() -> {
      if (getAll) {
        return cache.getAll(Arrays.asList(1, 4, 5)).get(1);
      }
      else {
        return cache.get(1);
      }
    });
    assertEquals("newValue_1", value2);
    assertEquals("newValue_2", RunContexts.empty().call(() -> cache.get(2)));
    assertEquals("newValue_3", RunContexts.empty().call(() -> cache.get(3)));
    assertEquals("newValue_4", RunContexts.empty().call(() -> cache.get(4)));
    assertEquals("newValue_5", RunContexts.empty().call(() -> cache.get(5)));
  }

  @Test
  public void testFuzzy() {
    testFuzzy(1000, false);
  }

  @Test
  public void testFuzzy_transactionalFastForward() {
    testFuzzy(1000, true);
  }

  /**
   * Tests concurrent calls of {@link AbstractMapTransactionMember#commitChanges}. Transactional data source provided by
   * {@link P_TransactionalData}; in a real setup this is usually a database.
   */
  @SuppressWarnings("unchecked")
  private void testFuzzy(int runs, boolean withTransactionalFastForward) {
    String CACHE_KEY = "CACHE_KEY";

    P_TransactionalData data = new P_TransactionalData();

    ICache<String, String> cache = BEANS.get(ICacheBuilder.class)
        .withCacheId("BasicCacheTestCacheId#testFuzzy[" + withTransactionalFastForward + "]")
        .withValueResolver((ICacheValueResolver<String, String>) key -> data.getCacheValue())
        .withTransactional(true)
        .withTransactionalFastForward(withTransactionalFastForward)
        .build();

    RunContexts.empty().run(() -> assertEquals("value_0_0", cache.get(CACHE_KEY)));
    Phaser startPhaser = new Phaser(4);

    IFuture<Void> future1 = Jobs.schedule(() -> {
      startPhaser.arriveAndAwaitAdvance();
      int[] ii = new int[1];
      for (int i = 0; i < runs; i++) {
        ii[0] = i;
        Jobs.schedule(() -> { // use jobs#schedule for randomness
          data.setValue1(ii[0]);
          cache.invalidate(new KeyCacheEntryFilter<>(Collections.singleton(CACHE_KEY)), true);
          data.assertCacheValue(cache, CACHE_KEY, "T1");
        }, Jobs.newInput().withThreadName("T1").withName("T1").withRunContext(RunContexts.empty())).awaitDoneAndGet();
      }
    }, Jobs.newInput().withRunContext(RunContexts.empty()));

    IFuture<Void> future2 = Jobs.schedule(() -> {
      startPhaser.arriveAndAwaitAdvance();
      int[] ii = new int[1];
      for (int i = 0; i < runs; i++) {
        ii[0] = i;
        Jobs.schedule(() -> { // use jobs#schedule for randomness
          data.setValue2(ii[0] * 10 + 2);
          cache.invalidate(new KeyCacheEntryFilter<>(Collections.singleton(CACHE_KEY)), true);
        }, Jobs.newInput().withThreadName("T2").withName("T2").withRunContext(RunContexts.empty())).awaitDoneAndGet();
      }
    }, Jobs.newInput().withRunContext(RunContexts.empty()));

    IFuture<Void> future3 = Jobs.schedule(() -> {
      startPhaser.arriveAndAwaitAdvance();
      for (int i = 0; i < runs; i++) {
        List<IFuture<Void>> futures = IntStream.range(0, 3)
            .mapToObj(ff -> Jobs.schedule(() -> data.assertCacheValue(cache, CACHE_KEY, "T3"), Jobs.newInput().withThreadName("T3").withName("T3").withRunContext(RunContexts.empty())))
            .collect(Collectors.toList());
        futures.forEach(IFuture::awaitDoneAndGet);
      }
    }, Jobs.newInput().withRunContext(RunContexts.empty()));

    startPhaser.arriveAndAwaitAdvance();
    future1.awaitDoneAndGet();
    future2.awaitDoneAndGet();
    future3.awaitDoneAndGet();
  }

  private static class P_TransactionalData {
    private final Lock m_value1CommitLock = new ReentrantLock();
    private final Lock m_value2CommitLock = new ReentrantLock();

    private boolean m_canceled;
    private int m_committedValue1;
    private int m_committedValue2;

    public void setValue1(int value1) {
      getTransactionMember(true).m_value1 = value1;
    }

    public void setValue2(int value2) {
      getTransactionMember(true).m_value2 = value2;
    }

    public String getCacheValue() {
      m_value1CommitLock.lock();
      m_value2CommitLock.lock();
      try {
        return getCacheValueWithoutLock();
      }
      finally {
        m_value2CommitLock.unlock();
        m_value1CommitLock.unlock();
      }
    }

    public void assertCacheValue(ICache<String, String> cache, String key, String context) {
      m_value1CommitLock.lock();
      m_value2CommitLock.lock();
      try {
        if (m_canceled) {
          throw new FutureCancelledError("canceled");
        }
        String expected = getCacheValueWithoutLock();
        String actual = cache.get(key);
        if (!expected.equals(actual)) {
          m_canceled = true;
          throw new ComparisonFailure("[" + context + "]", expected, actual);
        }
      }
      finally {
        m_value2CommitLock.unlock();
        m_value1CommitLock.unlock();
      }
    }

    private String getCacheValueWithoutLock() {
      int value1 = m_committedValue1;
      int value2 = m_committedValue2;
      P_TransactionalDataTransactionMember tm = getTransactionMember(false);
      if (tm != null) {
        if (tm.m_value1 != null) {
          value1 = tm.m_value1;
        }
        if (tm.m_value2 != null) {
          value2 = tm.m_value2;
        }
      }
      return "value_" + value1 + "_" + value2;
    }

    public P_TransactionalDataTransactionMember getTransactionMember(boolean createIfNotExist) {
      ITransaction t = ITransaction.CURRENT.get();
      if (t == null) {
        return null;
      }
      P_TransactionalDataTransactionMember m = (P_TransactionalDataTransactionMember) t.getMember(P_TransactionalDataTransactionMember.TRANSACTION_MEMBER_ID);
      if (m == null && createIfNotExist) {
        m = new P_TransactionalDataTransactionMember();
        t.registerMember(m);
      }
      return m;
    }

    private class P_TransactionalDataTransactionMember extends AbstractTransactionMember {
      private static final String TRANSACTION_MEMBER_ID = "P_TransactionalDataTransactionMember";
      private Integer m_value1;
      private Integer m_value2;
      private boolean m_committed;

      public P_TransactionalDataTransactionMember() {
        super(TRANSACTION_MEMBER_ID);
      }

      @Override
      public boolean needsCommit() {
        return true;
      }

      @Override
      public boolean commitPhase1() {
        if (m_value1 != null) {
          m_value1CommitLock.lock();
          m_committedValue1 = m_value1;
        }
        if (m_value2 != null) {
          m_value2CommitLock.lock();
          m_committedValue2 = m_value2;
        }
        m_committed = true;
        return true;
      }

      @Override
      public void release() {
        if (!m_committed) {
          return;
        }
        if (m_value1 != null) {
          m_value1CommitLock.unlock();
        }
        if (m_value2 != null) {
          m_value2CommitLock.unlock();
        }
        if (m_canceled) {
          throw new FutureCancelledError("canceled");
        }
      }
    }
  }
}
