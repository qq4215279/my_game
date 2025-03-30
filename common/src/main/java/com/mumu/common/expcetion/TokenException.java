/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.common.expcetion;

import lombok.Data;

/**
 * TokenException
 * token过期异常
 * @author liuzhen
 * @version 1.0.0 2025/3/30 14:22
 */
@Data
public class TokenException  extends Exception {
  private static final long serialVersionUID = 1L;

  private boolean expire;

  public TokenException(String message, Throwable cause) {
    super(message, cause);
  }

  public TokenException(String message) {
    super(message);
  }

  public boolean isExpire() {
    return expire;
  }

  public void setExpire(boolean expire) {
    this.expire = expire;
  }
}
