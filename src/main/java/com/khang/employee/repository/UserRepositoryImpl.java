package com.khang.employee.repository;

import com.khang.employee.entity.User;

import java.sql.SQLException;

public class UserRepositoryImpl extends RepositoryFactory<User, Long> implements UserRepository {
    public UserRepositoryImpl(String dbUrl, String dbUser, String dbPassword) throws SQLException {
        super(User.class, dbUrl, dbUser, dbPassword);
    }
}
