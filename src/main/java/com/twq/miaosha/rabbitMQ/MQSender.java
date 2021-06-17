package com.twq.miaosha.rabbitMQ;

import com.twq.miaosha.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
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
public class MQSender {
    private static Logger log = LoggerFactory.getLogger(MQSender.class);
    @Autowired
    AmqpTemplate amqpTemplate;

    public void send(Object message){
        log.info("send message is "+message);
        String msg = RedisService.beanToStr(message);
        amqpTemplate.convertAndSend(MQConfig.QUEUE,msg);
    }


    public void sendTopic(Object message){
        log.info("send message is "+message);
        String msg = RedisService.beanToStr(message);
        // tpic.key1这个类型topic就会转发到队列1中, 队列2中 因为队列2是通配符topic
        amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE,"topic.key1",msg+"1");
        //  tpic.key2 实际上是 定义的tpic.#这种主题，就会转发到队列2中
        amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE,"topic.key2",msg+"2");
    }


    public void sendFanout(Object message){
        log.info("send  fan out message is "+message);
        String msg = RedisService.beanToStr(message);
        // 会将消息发布到所有和 MQConfig.FANOUT_EXCHANGE交换器绑定的队列中。
        amqpTemplate.convertAndSend(MQConfig.FANOUT_EXCHANGE,"",msg+"1");

    }

    public void sendHeader(Object message){
        log.info("send  fan out message is "+message);
        String msg = RedisService.beanToStr(message);
        MessageProperties messageProperties = new MessageProperties();
        // 在binding函数中设置转发到队列中的俩个条件
        messageProperties.setHeader("head1","value1");
        messageProperties.setHeader("head2","value2");
        // 将msg和条件封装到一起。 然后发送到队列。
        Message obj = new Message(msg.getBytes(),messageProperties);
        // 会将消息发布到所有和 MQConfig.FANOUT_EXCHANGE交换器绑定的队列中。
        amqpTemplate.convertAndSend(MQConfig.HEADER_EXCHANGE,"",obj);

    }

    /**
     * 秒杀消息入队  使用Direct模式的
     * @param miaoshaMessage
     */
    public void sendMiaoShaMessage(MiaoshaMessage miaoshaMessage) {
        log.info("send  maioshaMessage is "+miaoshaMessage.getGoodsId()+":"+miaoshaMessage.getUser().getId());
        String message = RedisService.beanToStr(miaoshaMessage);
        amqpTemplate.convertAndSend(MQConfig.MIAOSHA_QUEUE,message);
    }
}
