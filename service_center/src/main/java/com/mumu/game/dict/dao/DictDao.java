package com.mumu.game.dict.dao;

import com.mumu.game.dict.entity.Dict;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * DictDao
 * 字典Dao
 * @author liuzhen
 * @version 1.0.0 2025/2/17 16:27
 */
@Mapper
public interface DictDao extends MongoRepository<Dict, String> {

  /**
   * 查询字典值
   * @param key key
   * @return java.lang.String
   * @since 2025/2/17 16:33
   */
  default String getValue(String key) {
    Dict dict = findById(key).orElse(null);
    if (dict == null) {
      return "";
    }
    return dict.getDictValue();
  }

  /**
   * 插入dict
   * @param key key
   * @param value value
   * @param desc desc
   * @since 2025/2/17 16:33
   */
  default void insertDict(String key, String value, String desc) {
    Dict dict = new Dict();
    dict.setDictKey(key);
    dict.setDictValue(value);
    dict.setDescription(desc);
    insert(dict);
  }

  /**
   * 更新dict
   * @param key key
   * @param value value
   * @since 2025/2/17 16:41
   */
  default void updateDict(String key, String value) {
    // TODO
  }

}
