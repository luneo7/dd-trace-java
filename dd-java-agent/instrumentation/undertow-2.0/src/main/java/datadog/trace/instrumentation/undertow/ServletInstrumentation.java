package datadog.trace.instrumentation.undertow;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import net.bytebuddy.asm.Advice;

import javax.servlet.ServletRequest;

import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.named;
import static datadog.trace.bootstrap.instrumentation.decorator.HttpServerDecorator.DD_SPAN_ATTRIBUTE;
import static datadog.trace.instrumentation.undertow.UndertowDecorator.DD_UNDERTOW_SPAN;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;

@AutoService(Instrumenter.class)
public final class ServletInstrumentation extends Instrumenter.Tracing implements Instrumenter.ForSingleType {

  public ServletInstrumentation() {
    super("undertow", "undertow-2.0");
  }

  @Override
  public String instrumentedType() {
    return "io.undertow.servlet.handlers.ServletInitialHandler";
  }

  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
    transformation.applyAdvice(
        isMethod().and(named("dispatchRequest")),
        getClass().getName() + "$DispatchAdvice");
  }

  @Override
  public String[] helperClassNames() {
    return new String[]{
        packageName + ".HttpServerExchangeURIDataAdapter",
        packageName + ".UndertowDecorator",
        packageName + ".UndertowExtractAdapter",
        packageName + ".UndertowExtractAdapter$Request",
        packageName + ".UndertowExtractAdapter$Response"
    };
  }

  public static class DispatchAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.Argument(0) final HttpServerExchange exchange,
        @Advice.Argument(1) final ServletRequestContext servletRequestContext) {
      AgentSpan undertow_span = exchange.getAttachment(DD_UNDERTOW_SPAN);
      if (null != undertow_span) {
        // Set the DD_SPAN_ATTRIBUTE so the servlet insturmentation does not create a new span
        // TODO CRG is this right?
        ServletRequest request = servletRequestContext.getServletRequest();
        request.setAttribute(DD_SPAN_ATTRIBUTE, undertow_span);
        // TODO CRG Should the Servlet Decorator onRequest get run to change
        // values?????? If so which one?
      }
    }
  }
}
