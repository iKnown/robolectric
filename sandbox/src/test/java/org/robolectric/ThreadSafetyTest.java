package org.robolectric;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.annotation.internal.Instrument;
import org.robolectric.internal.SandboxTestRunner;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.internal.bytecode.SandboxConfig;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SandboxTestRunner.class)
public class ThreadSafetyTest {
  @Test
  @SandboxConfig(shadows = {InstrumentedThreadShadow.class})
  public void shadowCreationShouldBeThreadsafe() throws Exception {
    Field field = InstrumentedThread.class.getDeclaredField("shadowFromOtherThread");
    field.setAccessible(true);

    for (int i = 0; i < 100; i++) { // :-(
      InstrumentedThread instrumentedThread = new InstrumentedThread();
      instrumentedThread.start();
      Object shadowFromThisThread = ShadowExtractor.extract(instrumentedThread);

      instrumentedThread.join();
      Object shadowFromOtherThread = field.get(instrumentedThread);
      assertThat(shadowFromThisThread).isSameAs(shadowFromOtherThread);
    }
  }

  @Instrument
  public static class InstrumentedThread extends Thread {
    InstrumentedThreadShadow shadowFromOtherThread;

    @Override
    public void run() {
      shadowFromOtherThread = (InstrumentedThreadShadow) ShadowExtractor.extract(this);
    }
  }

  @Implements(InstrumentedThread.class)
  public static class InstrumentedThreadShadow {
    @RealObject InstrumentedThread realObject;
    @Implementation
    protected void run() {
      Shadow.directlyOn(realObject, InstrumentedThread.class, "run");
    }
  }
}