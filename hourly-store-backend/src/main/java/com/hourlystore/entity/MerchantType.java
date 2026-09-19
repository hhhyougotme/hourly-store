package com.hourlystore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("merchant_type")
public class MerchantType {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String icon;
    @TableField("sort_order")
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
