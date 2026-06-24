package com.mumu.game.core.rpc.core;

import com.mumu.game.rpcproto.AbstractRpcResponseMessage;

/**
 * RpcCallBack
 * rpc回调
 * @author liuzhen
 * @version 1.0.0 2026/6/24 16:03
 */
public interface RpcCallBack {

    void callback(RpcFuture<? extends AbstractRpcResponseMessage> future);
}
