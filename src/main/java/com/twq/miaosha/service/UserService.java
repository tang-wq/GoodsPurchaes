package com.twq.miaosha.service;

import com.twq.miaosha.dao.UserDao;
import com.twq.miaosha.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    UserDao userDao;

    public User getItById(int id){
        return userDao.getById(id);
    }
}
