package io.microgenie.application.http;

import java.io.Closeable;

import com.google.common.base.Function;

public interface HttpExecutor<I, O>  extends Function<I, O>, Closeable{}