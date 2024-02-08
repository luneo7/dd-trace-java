package com.datadog.debugger.probe;

import static java.util.Collections.emptyList;

import com.datadog.debugger.el.ProbeCondition;
import com.datadog.debugger.exception.ExceptionProbeManager;
import com.datadog.debugger.exception.Fingerprinter;
import com.datadog.debugger.instrumentation.InstrumentationResult;
import com.datadog.debugger.sink.Snapshot;
import datadog.trace.bootstrap.debugger.CapturedContext;
import datadog.trace.bootstrap.debugger.MethodLocation;
import datadog.trace.bootstrap.debugger.ProbeId;
import datadog.trace.bootstrap.debugger.ProbeImplementation;
import datadog.trace.bootstrap.debugger.ProbeLocation;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionProbe extends LogProbe {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionProbe.class);
  private final transient ExceptionProbeManager exceptionProbeManager;

  public ExceptionProbe(
      ProbeId probeId,
      Where where,
      ProbeCondition probeCondition,
      Capture capture,
      Sampling sampling,
      ExceptionProbeManager exceptionProbeManager) {
    super(
        LANGUAGE,
        probeId,
        null,
        where,
        MethodLocation.EXIT,
        null,
        null,
        true,
        probeCondition,
        capture,
        sampling);
    this.exceptionProbeManager = exceptionProbeManager;
  }

  @Override
  public boolean isLineProbe() {
    // Exception probe are always method probe even if there is a line number
    return false;
  }

  @Override
  public CapturedContext.Status createStatus() {
    return new ExceptionProbeStatus(this);
  }

  @Override
  public void evaluate(
      CapturedContext context, CapturedContext.Status status, MethodLocation methodLocation) {
    if (!(status instanceof ExceptionProbeStatus)) {
      throw new IllegalStateException("Invalid status: " + status.getClass());
    }
    if (methodLocation != MethodLocation.EXIT) {
      return;
    }
    if (context.getCapturedThrowable() == null) {
      return;
    }
    Throwable throwable = context.getCapturedThrowable().getThrowable();
    String fingerprint =
        Fingerprinter.fingerprint(throwable, exceptionProbeManager.getClassNameFiltering());
    if (exceptionProbeManager.shouldCaptureException(fingerprint)) {
      LOGGER.debug("Capturing exception matching fingerprint: {}", fingerprint);
      // capture only on uncaught exception matching the fingerprint
      ExceptionProbeStatus exceptionStatus = (ExceptionProbeStatus) status;
      exceptionStatus.setCapture(true);
      super.evaluate(context, status, methodLocation);
    }
  }

  @Override
  public void commit(
      CapturedContext entryContext,
      CapturedContext exitContext,
      List<CapturedContext.CapturedThrowable> caughtExceptions) {
    Snapshot snapshot = createSnapshot();
    boolean shouldCommit = fillSnapshot(entryContext, exitContext, caughtExceptions, snapshot);
    if (shouldCommit) {
      snapshot.recordStackTrace(5);
      // add snapshot for later to wait for triggering point (ExceptionDebugger::handleException)
      exceptionProbeManager.addSnapshot(snapshot);
      LOGGER.debug(
          "committing exception probe id={}, snapshot id={}, exception id={}",
          id,
          snapshot.getId(),
          snapshot.getExceptionId());
    }
  }

  @Override
  public void buildLocation(InstrumentationResult result) {
    String type = where.getTypeName();
    String method = where.getMethodName();
    if (result != null) {
      type = result.getTypeName();
      method = result.getMethodName();
    }
    // drop line number for exception probe
    this.location = new ProbeLocation(type, method, where.getSourceFile(), emptyList());
  }

  public static class ExceptionProbeStatus extends LogStatus {
    private boolean capture;

    public ExceptionProbeStatus(ProbeImplementation probeImplementation) {
      super(probeImplementation);
    }

    public void setCapture(boolean capture) {
      this.capture = capture;
    }

    @Override
    public boolean shouldSend() {
      return super.shouldSend() && capture;
    }
  }
}
