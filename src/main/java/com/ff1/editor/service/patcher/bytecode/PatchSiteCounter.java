package com.ff1.editor.service.patcher.bytecode;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.Builder;

@Builder
record PatchSiteCounter(AtomicInteger siteCount) {

  public static PatchSiteCounter create() {
    return PatchSiteCounter.builder().build();
  }

  public PatchSiteCounter {
    siteCount = siteCount == null ? new AtomicInteger() : siteCount;
  }

  void increment() {
    siteCount.getAndIncrement();
  }

  int count() {
    return siteCount.get();
  }
}
