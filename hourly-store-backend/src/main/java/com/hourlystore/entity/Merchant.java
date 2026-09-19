package com.hourlystore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("merchant")
public class Merchant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantTypeId;
    private String name;
    private String address;
    /** Merchant service scope / offerings (thesis R11). */
    private String serviceDescription;
    private BigDecimal score;
    private Integer averagePrice;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
