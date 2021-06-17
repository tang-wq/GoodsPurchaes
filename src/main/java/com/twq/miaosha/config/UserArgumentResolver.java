package com.twq.miaosha.config;

import com.mysql.cj.util.StringUtils;
import com.twq.miaosha.access.UserContext;
import com.twq.miaosha.domain.MiaoshaUser;
import com.twq.miaosha.service.MiaoshaUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created with IntelliJ IDEA.
 *
 * 定义方法参数解析器
 * 对MiaoshaUser的处理逻辑进行封装
 * 在前端请求接口时，会因为webConfig，而处理这里的逻辑，从而进行MiaoshaUser参数的逻辑处理，
 * Controller中路由接口中MiaoshaUser参数的值实际上都是在这里完成解析赋值的
 * @Author: tangwq
 * @Date: 2021/05/05/15:49
 * @Description:
 */
@Service
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    MiaoshaUserService miaoshaUserService;

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        // 查看我们接收到的参数是不是存在MiaoshaUser类型的，如果是 才会做 resolveArgument()这个操作。
        Class<?> clazz = methodParameter.getParameterType();
        return clazz== MiaoshaUser.class;
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
//        HttpServletRequest httpServletRequest = nativeWebRequest.getNativeRequest(HttpServletRequest.class);
//        HttpServletResponse httpServletResponse = nativeWebRequest.getNativeResponse(HttpServletResponse.class);
//
//        // 获取前端传来的token参数
//        String paraToken = httpServletRequest.getParameter(MiaoshaUserService.COOKIE_NAME_TOKRN);
//        // 获取cookie中的Token
//        String cookieToken = getCookieToken(httpServletRequest,MiaoshaUserService.COOKIE_NAME_TOKRN);
//        // 如果 获取的俩个token都为空 则返回null。
//        if(StringUtils.isNullOrEmpty(cookieToken)&&StringUtils.isNullOrEmpty(paraToken)){
//            return null;
//        }
//        // 优先获取参数传递过来的token。
//        String token = StringUtils.isNullOrEmpty(paraToken)?cookieToken:paraToken;
//        MiaoshaUser miaoshaUser = miaoshaUserService.getByToken(httpServletResponse,token);
//        return miaoshaUser;

        /**
         * 因为我们在拦截器中 获取了User ，而拦截器是在这个参数解析之前执行的，
         * 因此我们在拦截器获取user后将其存入了ThreadLocal中（线程私有的参数）
         * 所以我们可以在这里直接获得。
         */
        return UserContext.getUser();
    }

    private String getCookieToken(HttpServletRequest httpServletRequest, String cookieNameTokrn) {
        // 获取httpServletRequest所有的cookie（应该也是浏览器的所有cookie）
       Cookie[] cookies =  httpServletRequest.getCookies();
       if(cookies==null||cookies.length<=0){
           return null;
       }
       // 遍历该cookies 看是否存在我们需要的cookie
        for (Cookie co: cookies) {
            if(co.getName().equals(cookieNameTokrn)){
                return co.getValue();
            }
        }

        return null;
    }
}
