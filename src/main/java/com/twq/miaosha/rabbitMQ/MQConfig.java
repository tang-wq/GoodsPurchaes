package com.twq.miaosha.rabbitMQ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: tangwq
 * @Date: 2021/05/08/15:43
 * @Description:
 */
@Configuration
public class MQConfig {

    // 这些只是定义的交换机和队列的名字， 具体是什么形式，是根据定义的交换器类型和队列绑定方法，
    public static final String QUEUE="queue";
    public static final String TOPIC_QUEUE1="topic.queue1";
    public static final String TOPIC_QUEUE2="topic.queue2";
    public static final String HEADER_QUEUE="header.queue";
    public static final String MIAOSHA_QUEUE="miaoshaQueue";


    public static final String TOPIC_EXCHANGE="topicExchange";
    public static final String FANOUT_EXCHANGE = "fanoutExchange";
    public static final String HEADER_EXCHANGE="headerExchange";


    /**
     * * 最直接的模式 direct模式  交换器exchange  直接将数据发送到交换器，交换机直接发送到队列中，做了一个路由，不需要绑定操作
     *
     *  使用默认的交换器
     * @return
     */
    @Bean
    public Queue queue(){
        // 第二个参数的意思是是否做持久化
        return new Queue(MIAOSHA_QUEUE,true);
    }

    /**
     * Topic 模式  主题与队列绑定。 对应主题的消息放入对应的队列中。
     * 初始化topic队列1
     * @return
     */
    @Bean
    public Queue topicQueue1(){
        return new Queue(TOPIC_QUEUE1,true);
    }

    /**
     * 初始化topic队列2
     * @return
     */
    @Bean
    public Queue topicQueue2(){
        return new Queue(TOPIC_QUEUE2,true);
    }

    /**
     * 定义TopicExchange交换器，用于中转
     * @return
     */
    @Bean
    public TopicExchange topicExchange(){
        return new TopicExchange(TOPIC_EXCHANGE);
    }

    /**
     * 主题与队列绑定
     * @return
     */
    @Bean
    public Binding topicBinDing1(){
        // topic.key1这种类型的topic绑定在topicQueue1这个队列中
        // 也就是说当发送的消息的topic（routingKey）是topic.key1，他就会放入我们建立的队列1中
        return BindingBuilder.bind(topicQueue1()).to(topicExchange()).with("topic.key1");
    }

    @Bean
    public Binding topicBinDing2(){
        // topic.#这种类型的topic绑定在topicQueue2这个队列中
        // 也就是说当发送的消息的topic（routingKey）是topic.#，也就是说所有以topic.开头为主题的消息都会会放入我们建立的队列2中
        //rabbitmq 的topic支持通配符 *标识一个单词，#标识一个或多个。
        return BindingBuilder.bind(topicQueue2()).to(topicExchange()).with("topic.#");
    }


    /**
     * fanout模式  广播模式  会将消息推送到和交换机绑定的虽有队列中？
     */

    /**
     * 创建fanoutExchange交换器
     * @return
     */
    @Bean
    public FanoutExchange fanoutExchange(){
        return new FanoutExchange(FANOUT_EXCHANGE);
    }

    //绑定队列到交换器
    @Bean
    public Binding fanoutBinding1(){
        return BindingBuilder.bind(topicQueue1()).to(fanoutExchange());
    }

    //绑定队列到交换器
    @Bean
    public Binding fanoutBinding2(){
        return BindingBuilder.bind(topicQueue2()).to(fanoutExchange());
    }

    /**
     * header模式  headExchange交换器
     *
     * 当发送到队列message 满足我们写定在Binding方法中的限制条件时，才会发送到绑定的队列中
     */

    @Bean
    public HeadersExchange headersExchange(){
        return new HeadersExchange(HEADER_EXCHANGE);
    }

    @Bean
    public Queue headerQueue(){
        return new Queue(HEADER_QUEUE);
    }

    @Bean
    public Binding headerBinding(){
        Map<String,Object> map = new HashMap<>();
        map.put("head1","value1");
        map.put("head2","value2");

        // .where .whereAll .whereAny 就是限定的模式
        // .whereAll 就是message满足map中的俩个key-value才会发送到对应的队列中。 map就是限制条件
        // .whereAny 满足任意一个即可
        return BindingBuilder.bind(headerQueue()).to(headersExchange()).whereAll(map).match();
    }



}
