package com.twq.miaosha.controller;

import com.twq.miaosha.Redis.UserKey;
import com.twq.miaosha.domain.User;
import com.twq.miaosha.result.CodeMsg;
import com.twq.miaosha.result.Result;
import com.twq.miaosha.service.MiaoshaUserService;
import com.twq.miaosha.service.RedisService;
import com.twq.miaosha.service.UserService;
import com.twq.miaosha.vo.LoginVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@Controller
@RequestMapping("/Login")
public class LoginController {

    private static Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    MiaoshaUserService miaoshaUserService;

    @Autowired
    RedisService redisService;

    /**
     * 跳转到Login页面
     * @return
     */
    @RequestMapping("/to_login")
    public String toLogin(){
        return "login";
    }

    /**
     * @Valid 这个注解是启动参数校验的意思 jsr
     *
     * @param loginVo
     * @return
     */
    @RequestMapping("/do_login")
    @ResponseBody
    public Result<String> doLogin(HttpServletResponse response, @Valid LoginVo loginVo){
        log.info(loginVo.toString());
        String token = miaoshaUserService.login(response,loginVo);
        // 因为登录失败等错误 都在service中当作GlobalException抛出，然后被异常处理器处理
        // 因此这里我们直接返回true即可。
        return Result.success(token);
    }


}
