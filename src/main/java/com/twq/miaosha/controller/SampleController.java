package com.twq.miaosha.controller;

import com.twq.miaosha.Redis.UserKey;
import com.twq.miaosha.domain.User;
import com.twq.miaosha.rabbitMQ.MQSender;
import com.twq.miaosha.result.Result;
import com.twq.miaosha.service.RedisService;
import com.twq.miaosha.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/demoController")
public class SampleController {

    @Autowired
    UserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    MQSender mqSender;

    @RequestMapping("/hello/thymeleaf")
    public String thymeleaf(Model model){
        model.addAttribute("name", "twq");
        return "hello";
    }

    @RequestMapping("/db/get")
    @ResponseBody
    public Result<String> dbGet(){
        User user = userService.getItById(1);
        return Result.success(user.getName());
    }

    @RequestMapping("/redis/getRedis")
    @ResponseBody
    public Result<User> redisGet(){
        User v1 = redisService.get(UserKey.getById,":"+2,User.class);
        return Result.success(v1);
    }

    @RequestMapping("/redis/setRedis")
    @ResponseBody
    public Result<Boolean> redisSet(){
        User user = new User();
        user.setId(1);
        user.setName("twq");
        Boolean v1 = redisService.set(UserKey.getById,":"+2,user);
        return Result.success(v1);
    }

    @RequestMapping("/mq")
    @ResponseBody
    public Result<String> mqTest(){
        mqSender.send("hello mq");
        mqSender.sendTopic("topic queue test");
        return Result.success("success mq");
    }

    @RequestMapping("/topic")
    @ResponseBody
    public Result<String> topicTest(){
        mqSender.sendTopic("topic queue test");
        return Result.success("success topic mq");
    }

    @RequestMapping("/fanout")
    @ResponseBody
    public Result<String> fanoutTest(){
        mqSender.sendFanout("fanout queue test");
        return Result.success("success fanout  mq");
    }


    @RequestMapping("/header")
    @ResponseBody
    public Result<String> headerTest(){
        mqSender.sendHeader("header queue test");
        return Result.success("success header  mq");
    }

}
