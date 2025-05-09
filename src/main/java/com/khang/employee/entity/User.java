package com.khang.employee.entity;

import com.khang.employee.annotations.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@KEntity
@KTable(name = "users")
@NoArgsConstructor
@Getter
@Setter
public class User {
    @KId
    @KColumn(name = "user_id")
    private Long id;

    @KColumn(name = "user_name")
    private String name;

    @KTransient
    private boolean temporaryFlag;

    public User(Long id, String name, boolean temporaryFlag) {
        this.id = id;
        this.name = name;
        this.temporaryFlag = temporaryFlag;
    }
}
