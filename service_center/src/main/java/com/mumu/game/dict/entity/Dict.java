package com.mumu.game.dict.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Dict
 * 字典
 * @author liuzhen
 * @version 1.0.0 2025/2/17 16:27
 */
@Data
@Document("dict")
public class Dict {
  /** 主键 */
  @Id()
  private String dictKey;
  /** 字典值 */
  private String dictValue;
  /** 描述 */
  private String description;

}
