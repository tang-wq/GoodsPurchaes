package com.twq.miaosha.rabbitMQ;

import com.twq.miaosha.domain.MiaoshaOrder;
import com.twq.miaosha.domain.MiaoshaUser;
import com.twq.miaosha.domain.OrderInfo;
import com.twq.miaosha.result.CodeMsg;
import com.twq.miaosha.result.Result;
import com.twq.miaosha.service.GoodsService;
import com.twq.miaosha.service.MiaoshaService;
import com.twq.miaosha.service.RedisService;
import com.twq.miaosha.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: tangwq
 * @Date: 2021/05/08/15:43
 * @Description:
 */
@Service
public class MReceive {

    private static Logger log = LoggerFactory.getLogger(MReceive.class);

    @Autowired
    GoodsService goodsService;

    @Autowired
    MiaoshaService miaoshaService;


    /**
     * 监听 MQConfig.QUEUE这个队列
     * @param message
     */
    @RabbitListener(queues = MQConfig.QUEUE)
    public void receive(String message){
        log.info("receive directQueue message is "+message);
    }

    /**
     * 监听TOPIC_QUEUE1这个队列
     * @param message
     */
    @RabbitListener(queues = MQConfig.TOPIC_QUEUE1)
    public void topicReceive1(String message){
        log.info("receive TopicQueue1 message is "+message);
    }

    /**
     * 监听TOPIC_QUEUE2这个队列
     * @param message
     */
    @RabbitListener(queues = MQConfig.TOPIC_QUEUE2)
    public void topicReceive2(String message){
        log.info("receive TopicQueue2 message is "+message);
    }


    /**
     * 监听HEADER_QUEUE这个队列
     *
     * header模式比较特殊
     * 因为我们在传输的时候 msg是 byte[]数组 所以接收也为byte[]数组
     *
     * @param message
     */
    @RabbitListener(queues = MQConfig.HEADER_QUEUE)
    public void headerReceive(byte[] message){
        log.info("receive HeaderQueue message is "+message.toString());
    }

    /**
     * 监听秒杀这个队列
     * @param message
     */
    @RabbitListener(queues = MQConfig.MIAOSHA_QUEUE)
    public void miaoshaReceive(String message){
        MiaoshaMessage miaoshaMessage = RedisService.StringToBean(message,MiaoshaMessage.class);
        MiaoshaUser miaoshaUser = miaoshaMessage.getUser();
        Long goodsId = miaoshaMessage.getGoodsId();

        //判断库存
        GoodsVo goods = goodsService.getGoodsVoById(goodsId);//10个商品，req1 req2
        int stock = goods.getStockCount();
        /**
         * 这里为什么还要判断 库存
         *
         * 因为消息队列也会出现重复消费的问题（能否在别的地方解决重复消费问题？）， 因此这里也判断一次库存。
         */
        if(stock <= 0) {
            return;
        }

        //减库存 下订单 写入秒杀订单
        OrderInfo orderInfo = miaoshaService.doMiaosha(miaoshaUser, goods);
    }

}
