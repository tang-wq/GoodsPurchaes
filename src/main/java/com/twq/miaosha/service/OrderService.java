package com.twq.miaosha.service;

import com.twq.miaosha.Redis.OrderKey;
import com.twq.miaosha.dao.GoodsDao;
import com.twq.miaosha.dao.OrderDao;
import com.twq.miaosha.domain.MiaoshaOrder;
import com.twq.miaosha.domain.MiaoshaUser;
import com.twq.miaosha.domain.OrderInfo;
import com.twq.miaosha.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: tangwq
 * @Date: 2021/05/06/19:55
 * @Description:
 */

@Service
public class OrderService {

    @Autowired
    OrderDao orderDao;

    @Autowired
    RedisService redisService;





    /**
     * 根据userId和商品Id获取秒杀订单。
     * @param userId
     * @param goodsId
     * @return
     */
    public MiaoshaOrder getMiaoshaOrderByUserIdAndGoodsId(Long userId, Long goodsId) {

        // 优化  下单时会将秒杀订单信息存入redis中，因此获取的时候也从redis获取，这样就不用去访问数据库了。
        return redisService.get(OrderKey.getMiaoshaOrderByUidGid,""+userId+"_"+goodsId,MiaoshaOrder.class);
        // 不用从数据库中获取
        // return  orderDao.getMiaoshaOrderByUserIdGoodsId(userId,goodsId);
    }

    /**
     * 创建订单
     * @param miaoshaUser
     * @param goodsVo
     * @return
     */
    @Transactional
    public OrderInfo createOrder(MiaoshaUser miaoshaUser, GoodsVo goodsVo) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCreateDate(new Date());
        orderInfo.setDeliveryAddrId(0L);
        orderInfo.setGoodsCount(1);
        orderInfo.setGoodsId(goodsVo.getId());
        orderInfo.setGoodsName(goodsVo.getGoodsName());
        orderInfo.setGoodsPrice(goodsVo.getMiaoshaPrice());
        orderInfo.setOrderChannel(1);
        orderInfo.setStatus(0);
        orderInfo.setUserId(miaoshaUser.getId());
        orderDao.insert(orderInfo);// Mybatis insert之后会自动给对象的ID赋值
        MiaoshaOrder miaoshaOrder = new MiaoshaOrder();
        miaoshaOrder.setGoodsId(goodsVo.getId());
        miaoshaOrder.setOrderId(orderInfo.getId());
        miaoshaOrder.setUserId(miaoshaUser.getId());
        orderDao.insertMiaoshaOrder(miaoshaOrder);

        // 秒杀订单存入redis，查询的时候可以直接从缓存查，从而减少数据库访问。
        redisService.set(OrderKey.getMiaoshaOrderByUidGid,""+miaoshaUser.getId()+"_"+goodsVo.getId(),miaoshaOrder);

        return orderInfo;

    }

    public OrderInfo getOrderById(long orderId) {
        return orderDao.getOrderById(orderId);
    }

}
