/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.game_netty.channel.future;

import com.mumu.framework.core.game_netty.channel.GameChannel;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * GameChannelFuture
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/30 16:53
 */
public interface GameChannelFuture extends Future<Void> {

    GameChannel channel();

    @Override
    GameChannelFuture addListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    GameChannelFuture addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    GameChannelFuture removeListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    GameChannelFuture removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    GameChannelFuture sync() throws InterruptedException;

    @Override
    GameChannelFuture syncUninterruptibly();

    @Override
    GameChannelFuture await() throws InterruptedException;

    @Override
    GameChannelFuture awaitUninterruptibly();
}
