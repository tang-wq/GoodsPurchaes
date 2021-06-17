package com.twq.miaosha.controller;

import com.twq.miaosha.domain.MiaoshaUser;
import com.twq.miaosha.result.Result;
import com.twq.miaosha.vo.GoodsVo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: tangwq
 * @Date: 2021/05/07/10:54
 * @Description:
 */
@Controller
@RequestMapping("/test")
public class TestController {

    @RequestMapping("/info")
    @ResponseBody
    public Result<MiaoshaUser> toGoos(MiaoshaUser miaoshaUser){

        return Result.success(miaoshaUser);
    }
}
