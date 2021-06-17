package com.twq.miaosha.dao;

import com.twq.miaosha.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserDao {

    @Select("select * from test_table where id=#{id}")
    public User getById(@Param("id") int id);
}
