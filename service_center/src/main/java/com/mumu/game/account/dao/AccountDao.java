package com.mumu.game.account.dao;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mumu.game.account.entity.AccountEntity;

/**
 * AccountDao
 * 账号数据访问层
 * @author liuzhen
 * @version 1.0.0 2026/8/2 14:10
 */
@Mapper
public interface AccountDao extends BaseMapper<AccountEntity> {

  /** getAccountEntity */
  default AccountEntity getAccountEntity(long playerId) {
    return selectById(playerId);
  }
}
