package com.hourlystore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    public static final int ROLE_USER = 0;
    public static final int ROLE_ADMIN = 1;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String email;
    private String passwordHash;
    private String nickname;
    private Integer status;
    private Integer role;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
