/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.game_netty.channel.future;

import com.mumu.framework.core.game_netty.channel.GameChannel;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

/**
 * GameChannelPromise
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/30 16:53
 */
public interface GameChannelPromise extends GameChannelFuture, Promise<Void> {
    @Override
    GameChannel channel();

    @Override
    GameChannelPromise setSuccess(Void result);

    GameChannelPromise setSuccess();

    boolean trySuccess();

    @Override
    GameChannelPromise setFailure(Throwable cause);

    @Override
    GameChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    GameChannelPromise addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    GameChannelPromise removeListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    GameChannelPromise removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    GameChannelPromise sync() throws InterruptedException;

    @Override
    GameChannelPromise syncUninterruptibly();

    @Override
    GameChannelPromise await() throws InterruptedException;

    @Override
    GameChannelPromise awaitUninterruptibly();
}
