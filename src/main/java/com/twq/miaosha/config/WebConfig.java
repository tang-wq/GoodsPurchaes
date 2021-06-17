package com.twq.miaosha.config;

import com.twq.miaosha.access.AccessInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * SpringMVC 支持的配置类。 这里可以配置MVC各种监听器 过滤器等
 *
 * @Author: tangwq
 * @Date: 2021/05/05/15:16
 * @Description:
 */

@Configuration
public class WebConfig extends WebMvcConfigurationSupport {

    @Autowired
    UserArgumentResolver userArgumentResolver;

    @Autowired
    AccessInterceptor accessInterceptor;

    /**
     * 注册参数解析器
     *
     * Controller方法中 Model,等很多参数为什么可以接收到前端的赋值呢？
     * 实际上SpringMVC底层都是通过这个方法去赋值的。
     * 因此我们可以通过重写这个方法，去给我们自定义Controller方法中的参数赋值。
     * @param argumentResolvers
     */
    @Override
    protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        // 注册到 argumentResolvers 这个列表里面
        argumentResolvers.add(userArgumentResolver);
        super.addArgumentResolvers(argumentResolvers);
    }


    /**
     * 注册拦截器
     * @param registry
     */
    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessInterceptor);
        super.addInterceptors(registry);
    }

    /**
     * 静态页 static文件夹下的加载步到失效， 在这里需要配置路径
     */
    private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
            "classpath:/META-INF/resources/", "classpath:/resources/",
            "classpath:/static/", "classpath:/public/","classpath:/img/" };


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!registry.hasMappingForPattern("/webjars/**")) {
            registry.addResourceHandler("/webjars/**").addResourceLocations(
                    "classpath:/META-INF/resources/webjars/");
        }
        if (!registry.hasMappingForPattern("/**")) {
            registry.addResourceHandler("/**").addResourceLocations(
                    CLASSPATH_RESOURCE_LOCATIONS);
        }

    }

}
