package datadog.trace.instrumentation.spymemcached;

import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import java.util.concurrent.ExecutionException;
import net.spy.memcached.internal.GetFuture;

public class GetCompletionListener extends CompletionListener<GetFuture<?>>
    implements net.spy.memcached.internal.GetCompletionListener {
  public GetCompletionListener(final AgentSpan span, final String methodName) {
    super(span, methodName);
  }

  @Override
  public void onComplete(final GetFuture<?> future) {
    closeAsyncSpan(future);
  }

  @Override
  protected void processResult(final AgentSpan span, final GetFuture<?> future)
      throws ExecutionException, InterruptedException {
    final Object result = future.get();
    setResultTag(span, result != null);
  }
}
