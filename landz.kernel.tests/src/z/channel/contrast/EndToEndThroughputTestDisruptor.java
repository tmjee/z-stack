/**
 * Copyright 2013, Landz and its contributors. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package z.channel.contrast;

import com.lmax.disruptor.*;
import org.junit.Test;

import static com.lmax.disruptor.RingBuffer.createSingleProducer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 */
public class EndToEndThroughputTestDisruptor {

  private static final int BUFFER_SIZE = 1024;
  private RingBuffer<ValueEvent> ringBuffer =
      createSingleProducer(ValueEvent.EVENT_FACTORY, BUFFER_SIZE, new YieldingWaitStrategy());

  private static final int RUNS = 500_000_000;
  public static final Object OBJECT = new Object();

  @Test
  public void testDisruptor() {
    runFinePrint("for Disruptor", () -> runDisruptor());
  }

  private void runDisruptor() {
//    CountDownLatch endLatch = new CountDownLatch(1);

    BatchEventProcessor<ValueEvent> processorC = new BatchEventProcessor<>(//BUG?: can not infer type if no diamond
        ringBuffer,
        ringBuffer.newBarrier(),
        new EventHandler<ValueEvent>() {
          @Override
          public void onEvent(ValueEvent event, long sequence, boolean endOfBatch) throws Exception {
            // Publishers claim events in sequence
//            assertThat(event.getValue(),is(OBJECT));
//            if (sequence==(RUNS-1))
//              endLatch.countDown();
          }
        }
    );

    // Each processor runs on a separate thread
    Thread t = new Thread(processorC);
    t.start();

    long s = System.nanoTime();
    for (int i = 0; i < RUNS; i++) {
      // Publishers claim events in sequence
      long seq = ringBuffer.next();
      ValueEvent event = ringBuffer.get(seq);
      event.setValue(OBJECT);
      // publish the event
      ringBuffer.publish(seq);
    }
//    try{
//      endLatch.await();
//    }catch (Exception e){e.printStackTrace();}

    long time = System.nanoTime()-s ;

    long opsPerSecond = RUNS * 1000_000_000L / time;
    System.out.printf("%,d ops/sec\n", opsPerSecond);
  }


  private static void runFinePrint(String label, Runnable runnable) {
    System.out.println("================================");
    System.out.println(label + " start...");
    runnable.run();
    System.out.println(label + " done.");
    System.out.println("================================");
  }


  public static void main(String[] args) {
    new EndToEndThroughputTestDisruptor().testDisruptor();
  }

  static final class ValueEvent {
    private Object value;

    public Object getValue() {
      return value;
    }

    public void setValue(final Object value) {
      this.value = value;
    }

    public static final EventFactory<ValueEvent> EVENT_FACTORY = new EventFactory<ValueEvent>() {
      public ValueEvent newInstance() {
        return new ValueEvent();
      }
    };
  }

}