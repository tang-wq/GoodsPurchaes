package com.twq.miaosha.controller;

import com.twq.miaosha.Redis.GoodsKey;
import com.twq.miaosha.access.AccessLimit;
import com.twq.miaosha.domain.MiaoshaOrder;
import com.twq.miaosha.domain.MiaoshaUser;
import com.twq.miaosha.domain.OrderInfo;
import com.twq.miaosha.rabbitMQ.MQSender;
import com.twq.miaosha.rabbitMQ.MiaoshaMessage;
import com.twq.miaosha.result.CodeMsg;
import com.twq.miaosha.result.Result;
import com.twq.miaosha.service.GoodsService;
import com.twq.miaosha.service.MiaoshaService;
import com.twq.miaosha.service.OrderService;
import com.twq.miaosha.service.RedisService;
import com.twq.miaosha.vo.GoodsVo;
import com.twq.miaosha.vo.LoginVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: tangwq
 * @Date: 2021/05/06/19:35
 * @Description:
 */
@Controller
@RequestMapping("miaosha")
public class MiaoshaController implements InitializingBean {

    private static Logger log = LoggerFactory.getLogger(MiaoshaController.class);

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    MiaoshaService miaoshaService;

    @Autowired
    RedisService redisService;

    @Autowired
    MQSender mqSender;

    /**
     * 本地内存标记， 标记redis预扣商品是否秒杀完成， 如果redis商品秒杀完成，就不需要再去访问redis，减少redis的压力。
     * 这里属于内存标记 因为HashMap初始化之后就是在内存的嘛。所以叫内存标记
     */
    private Map<Long,Boolean> goodsOverlocalMark = new HashMap<>();

    /**
     * 这个不是前置处理器， InitializingBean是在Bean初始化的时候执行，而前（后）置处理器 BeanPostProcessor，是在bean初始化前/后执行
     *
     * 为什么不用前置 处理器呢？  如果用前置处理器 bean还没有到初始化步骤， 因此Service方法不能注入 因此会出现问题。
     *
     *当class 继承 InitializingBean， 在这个bean进行初始画的时候， 会自动调用afterPropertiesSet这个方法。
     *
     * 因此我们在这里面进行秒杀商品的数量预热到redis， 也就是在bean初始化的时候进行预热。
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        //获取所有商品
        List<GoodsVo> goodsVoList =  goodsService.listGoodsVo();
        if(goodsVoList!=null){

            for(GoodsVo goodsVo:goodsVoList){
                // 将秒杀商品的数量存入redis， 以前置key+商品id作为redis的key
                redisService.set(GoodsKey.getGoodsStock,goodsVo.getId().toString(),goodsVo.getStockCount());
                // 初始化， 库存内存标记置为 false 表示该商品还有库存
                goodsOverlocalMark.put(goodsVo.getId(),false);
            }
        }
    }


    /**
     * 前后端不分离
     *
     * 使用Model封装数据之后， SpringMvc这种结构ModelAndView就会 将Model中的数据去渲染前端页面，然后显示在浏览器上。 渲染实际上是后端处理的
     * @param model
     * @param miaoshaUser
     * @param goodsId
     * @return
     */
    @RequestMapping("/do_miaosha1")
    public String doMiaosha1(Model model, MiaoshaUser miaoshaUser, @RequestParam("goodsId") Long goodsId){

        if(miaoshaUser==null){
            return "login";
        }

        GoodsVo goodsVo = goodsService.getGoodsVoById(goodsId);
        if(goodsVo.getStockCount()<=0){
            model.addAttribute("errmsg", CodeMsg.MIAO_SHA_OVER);
            return "miaosha_fail";
        }

        // 判断是否已秒杀到。
        MiaoshaOrder miaoshaOrder = orderService.getMiaoshaOrderByUserIdAndGoodsId(miaoshaUser.getId(),goodsId);

        // 判断是否已经秒杀到了， 防止相同用户对一件商品的重复秒杀。
        if(miaoshaOrder!=null){
            model.addAttribute("errmsg", CodeMsg.REPEATE_MIAOSHA);
            return "miaosha_fail";
        }
        // 减库存， 下订单 ， 写入秒杀订单 这三个操作应该是原子的，在一个事务里。
        OrderInfo orderInfo = miaoshaService.doMiaosha(miaoshaUser,goodsVo);
        model.addAttribute("orderInfo", orderInfo);
        model.addAttribute("goods", goodsVo);

        // 跳转订单详情页
        return "order_detail";

    }

    /**
     *
     * 优化前 秒杀的方法
     * 优化前直接操作数据库：QPS：800+
     *
     * 前后端分离后的秒杀接口
     *
     * 跳转等操作由前端自己去做，并且渲染等都是由前端去做，
     *
     * 基本不在需要SpringMVC 提供的Model和View去渲染了
     * 实际上 Model就是SpringMvc提供的封装数据 然后 去渲染页面。 这就是前后端不分离。
     *
     * 我们直接将数据返回给前端， 前端自己去处理数据 渲染页面。
     *
     * @param model
     * @param user
     * @param goodsId
     * @return
     */
    @RequestMapping(value="/do_miaosha_noYH", method= RequestMethod.POST)
    @ResponseBody
    public Result<OrderInfo> miaoshaNoYH(Model model,MiaoshaUser user,
                                     @RequestParam("goodsId")long goodsId) {
        model.addAttribute("user", user);
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        /**
         * 如果标记的商品库存为空（true）则直接返回秒杀结束
         * 不会再去请求redis，减少了对redis的访问。
         */
        if(goodsOverlocalMark.get(goodsId)){
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }

        //判断库存
        GoodsVo goods = goodsService.getGoodsVoById(goodsId);//10个商品，req1 req2
        int stock = goods.getStockCount();
        /**
         * 高并发下，这个判断可能不准确， 因为假设库存只有1了，此时第一个请求进来判断到库存还剩1，执行秒杀操作，但是还没修改数据库
         * 第二个请求过来判断库存还是剩1，也会执行秒杀操作。 因此在后面还需要判断是否修改库存成功，再让他产生订单。
         */
        if(stock <= 0) {
            // 优化， 库存没有了。 就将库存标记置为true，表示库存为空
            goodsOverlocalMark.put(goodsId,true);
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        //判断是否已经秒杀到了
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdAndGoodsId(user.getId(), goodsId);
        if(order != null) {
            return Result.error(CodeMsg.REPEATE_MIAOSHA);
        }
        //减库存 下订单 写入秒杀订单
        OrderInfo orderInfo = miaoshaService.doMiaosha(user, goods);

        /**
         * 在高并发下，只有减数据库成功的请求才会产生ID
         */
        if(orderInfo==null){
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        return Result.success(orderInfo);
    }

    /**
     * 优化后的DPS：1900+  这个也和服务器性能有关，，我自己电脑作为服务器，也是有瓶颈的。 所有QPS 不是很高
     * @param model
     * @param user
     * @param goodsId
     * @return
     */

    @RequestMapping(value="/{path}/do_miaoshaYH", method= RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaoshaYH(Model model,MiaoshaUser user,
                                         @RequestParam("goodsId")long goodsId,
                                     @PathVariable("path") String path) {
        model.addAttribute("user", user);
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }


        //验证path,做一层安全的优化， 防止恶意访问秒杀接口
        boolean check = miaoshaService.checkPath(user, goodsId, path);
        if(!check){
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }

        /**
         *  PS 这里有个问题， 也就是幂等问题（重复秒杀问题）
         *  就是一个用户 同时进来秒杀 那他等于一个人 减了俩次redis库存， 而后面只允许一个用户产生一个秒杀订单，也只允许秒杀一次
         *  那么redis数据是否需要回滚（或者说是库存+1）。
         *
         *  为什么会出现一个用户秒杀俩次？
         *  只是说可能性， TCP/IP有超时重传机制， 如果服务端没有即时返回消息，客户端就会重传一次完全相同的数据， 此时就出现重复秒杀
         *  这也是接口请求幂等的问题（MQ消息队列也会有重复消费的问题）。
         */
        // redis操作是原子性的（服务器端是单线程的），一个操作执行完才会执行另外一个请求。
        // 减少redis中的商品库存
        long stock = redisService.decr(GoodsKey.getGoodsStock,String.valueOf(goodsId));
        if(stock<0){ // 库存不足 则直接返回秒杀失败。 那么 没有秒杀成功的请求，就不会入消息队列，也不会去操作数据库
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }

        //判断是否已经秒杀到了， 防止同一用户重复秒杀 , 其实也就是处理幂等问题。
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdAndGoodsId(user.getId(), goodsId);
        if(order != null) {
            // 这里应该将减少的redis库存放回去，我觉得
            //long stock1 = redisService.incr(GoodsKey.getGoodsStock,String.valueOf(goodsId));
            return Result.error(CodeMsg.REPEATE_MIAOSHA);
        }

        /**
         * 秒杀到redis中库存的请求，入消息队列， 进行异步下单处理（操作数据库）。
         *  这样就只有秒杀到商品的请求才会操作数据库，大大减少了数据库的压力，并且是通过消息队列进行异步处理
         *  增加了用户的体验。
         */
        MiaoshaMessage miaoshaMessage = new MiaoshaMessage();
        miaoshaMessage.setUser(user);
        miaoshaMessage.setGoodsId(goodsId);
        mqSender.sendMiaoShaMessage(miaoshaMessage);

        return Result.success(0); // 返回0 表示排队中
    }


    /**
     * 用于秒杀入消息队列后，看是否后续下单（操作数据库）是否成功
     *
     *实际上就是查看redis是否有对应的订单生成， 如果下单成功，则会有订单在redis生成。
     * 如果库存没了，且redis没有订单生成，则说明下单失败。（一般来说不会出现这个问题）
     *
     * orderId：成功
     * -1：秒杀失败
     * 0： 排队中
     * */
    @RequestMapping(value="/result", method=RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(Model model,MiaoshaUser user,
                                      @RequestParam("goodsId")long goodsId) {
        model.addAttribute("user", user);
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        long result  =miaoshaService.getMiaoshaResult(user.getId(), goodsId);
        return Result.success(result);
    }


    /**
     * @AccessLimit 自定义注解， 设置接口的访问次数，
     * 在自定义拦截器AccessInterceptor进行限流逻辑处理
     *
     * getMiaoshaPath 目的是为了生成一个变量数据返回给前端
     * 然后前端拼接成秒杀接口，请求秒杀接口
     *
     * 秒杀接口在验证一下这个参数是否存在redis中， 起到安全的作用。
     *
     *
     * @param request
     * @param user
     * @param goodsId
     * @param verifyCode
     * @return
     */
    @AccessLimit(seconds=5, maxCount=5, needLogin=true)
    @RequestMapping(value="/path", method=RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaPath(HttpServletRequest request, MiaoshaUser user,
                                         @RequestParam("goodsId")long goodsId,
                                         @RequestParam(value="verifyCode", defaultValue="0")int verifyCode
    ) {
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        boolean check = miaoshaService.checkVerifyCode(user, goodsId, verifyCode);
        if(!check) {
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }
        String path  =miaoshaService.createMiaoshaPath(user, goodsId);
        return Result.success(path);
    }


    /**
     * 图形验证码
     * @param response
     * @param user
     * @param goodsId
     * @return
     */
    @RequestMapping(value="/verifyCode", method=RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaVerifyCod(HttpServletResponse response,MiaoshaUser user,
                                              @RequestParam("goodsId")long goodsId) {
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        try {
            BufferedImage image  = miaoshaService.createVerifyCode(user, goodsId);
            OutputStream out = response.getOutputStream();
            ImageIO.write(image, "JPEG", out);
            out.flush();
            out.close();
            return null;
        }catch(Exception e) {
            e.printStackTrace();
            return Result.error(CodeMsg.MIAOSHA_FAIL);
        }
    }


}
