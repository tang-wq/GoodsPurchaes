package com.twq.miaosha.controller;

import com.mysql.cj.util.StringUtils;
import com.twq.miaosha.Redis.GoodsKey;
import com.twq.miaosha.domain.Goods;
import com.twq.miaosha.domain.MiaoshaUser;
import com.twq.miaosha.result.Result;
import com.twq.miaosha.service.GoodsService;
import com.twq.miaosha.service.MiaoshaUserService;
import com.twq.miaosha.service.RedisService;
import com.twq.miaosha.vo.GoodsDetailVo;
import com.twq.miaosha.vo.GoodsVo;
import javafx.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: tangwq
 * @Date: 2021/04/30/19:23
 * @Description:
 */
@Controller
@RequestMapping(("/goods"))
public class GoodsController {
    private static Logger log = LoggerFactory.getLogger(GoodsController.class);

    @Autowired
    MiaoshaUserService miaoshaUserService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    RedisService redisService;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    ThymeleafViewResolver thymeleafViewResolver;

    /**
     *
     * @param model
     * @param cookieToken  获取登录时上传的cookie值。
     * @param paraToken  有些时候 token是直接通过参数传递过来的 不是通过 cookie获得的。
     * @return
     */
//    @RequestMapping("/to_goodList")
//    public String toGoos(HttpServletResponse response, Model model, @CookieValue(value = MiaoshaUserService.COOKIE_NAME_TOKRN,required = false) String cookieToken,
//                         @RequestParam(value = MiaoshaUserService.COOKIE_NAME_TOKRN,required = false) String paraToken){
//
//        // 如果 获取的俩个token都为空 则返回登陆界面。
//        if(StringUtils.isNullOrEmpty(cookieToken)&&StringUtils.isNullOrEmpty(paraToken)){
//            return "login";
//        }
//        // 优先获取参数传递过来的token。
//        String token = StringUtils.isNullOrEmpty(paraToken)?cookieToken:paraToken;
//        MiaoshaUser miaoshaUser = miaoshaUserService.getByToken(response,token);
//        model.addAttribute("MiaoshaUser",miaoshaUser);
//        return "goods_list";
//    }

    /**
     * QPS 800+
     * MYSQL 占用CPU很高
     *
     * 将上面的方法和Controller参数 我们进行了封装，最后只需要个MiaoshaUser即可
     *
     * 实际上相当于通过SpringMVC提供的WebMvcConfigurationSupport类，我们实现对路由方法中自定义参数的处理
     * 比如这里的MiaoshaUser参数，config/UserArgumentResolver和WebConfig 是封装的类。
     * 因此，他在调用这个方法前就会将MiaoshaUser处理好，传入对应的值。
     *
     * 封装是为了可以复用，MiaoshaUser这个对象以及其处理逻辑在很多地方都会用到， 因此将其封装起来。
     * 也不需要像上面那么多参数，都是封装的类里面做了处理。
     * @param model
     * @param miaoshaUser
     * @return
     */
    @RequestMapping("/to_goodList")
    public String toGoos(Model model, MiaoshaUser miaoshaUser){

        // 获得到的用户信息传递到前端，
        model.addAttribute("MiaoshaUser",miaoshaUser);

        // 查询商品列表
        List<GoodsVo> goodsVoList = goodsService.listGoodsVo();

        model.addAttribute("goodsList", goodsVoList);

        return "goods_list";
    }

    /**
     * QPS 1700+
     * MySql几乎不占用CPU 。 都是redis处理。
     *
     * 对toGoods方法的优化， toGoods每次请求都会访问数据库。
     * 页面优化， 第一次获取物品列表时，从数据库拿出数据，渲染页面，然后缓存物品列表页面，使得再次访问时不需要在访问数据库，
     * 而是直接访问缓存的页面数据，从而减少对数据库的访问。
     *
     * 这里值得注意的是 页面缓存，缓存的是之前的渲染数据， 因此缓存过期时间不能太长，一般来说是1分钟
     *
     * 并且值得注意的是，对于即时性很强的页面 不能够去做缓存， 比如秒杀的页面。 商品页面是可以做缓存的，订单页面也可以做缓存。
     *
     * @param request
     * @param response
     * @param model
     * @param user
     * @return
     */
    @RequestMapping(value="/to_list", produces="text/html;charset=utf8")
    @ResponseBody
    public String list(HttpServletRequest request, HttpServletResponse response, Model model, MiaoshaUser user){
        model.addAttribute("user", user);
        //取缓存
        String html = redisService.get(GoodsKey.getGoodsList, "", String.class);
        if(!StringUtils.isNullOrEmpty(html)) {
            return html;
        }
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        model.addAttribute("goodsList", goodsList);
//    	 return "goods_list";

        // Spring5 中这个方法过时 用IWebContext代替。
//        SpringWebContext ctx = new SpringWebContext(request,response,
//                request.getServletContext(),request.getLocale(), model.asMap(), applicationContext );

        IWebContext ctx =new WebContext(request,response,
                request.getServletContext(),request.getLocale(),model.asMap());
        //手动渲染, 在这里将页面渲染后，存入redis， 之前是传入到前端， SpringBoot自动帮我们渲染。
        html = thymeleafViewResolver.getTemplateEngine().process("goods_list", ctx);
        if(!StringUtils.isNullOrEmpty(html)) {
            redisService.set(GoodsKey.getGoodsList, "", html);
        }
        return html;
    }


    @RequestMapping("/to_detail/{goodsId}")
    @ResponseBody
    public Result detail(MiaoshaUser miaoshaUser, @PathVariable("goodsId") Long goodsId){
        GoodsVo goods = goodsService.getGoodsVoById(goodsId);
        long startAt = goods.getStartDate().getTime();
        long endAt = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();
        int miaoshaStatus = 0;
        int remainSeconds = 0;
        if(now < startAt ) {//秒杀还没开始，倒计时
            miaoshaStatus = 0;
            remainSeconds = (int)((startAt - now )/1000);
        }else  if(now > endAt){//秒杀已经结束
            miaoshaStatus = 2;
            remainSeconds = -1;
        }else {//秒杀进行中
            miaoshaStatus = 1;
            remainSeconds = 0;
        }
        GoodsDetailVo vo = new GoodsDetailVo();
        vo.setGoods(goods);
        vo.setUser(miaoshaUser);
        vo.setRemainSeconds(remainSeconds);
        vo.setMiaoshaStatus(miaoshaStatus);
        return Result.success(vo);
    }






}
