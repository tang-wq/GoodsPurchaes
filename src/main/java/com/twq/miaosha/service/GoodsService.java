package com.twq.miaosha.service;

import com.twq.miaosha.dao.GoodsDao;
import com.twq.miaosha.domain.MiaoshaGoods;
import com.twq.miaosha.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: tangwq
 * @Date: 2021/05/06/16:13
 * @Description:
 */
@Service
public class GoodsService {

    @Autowired
    GoodsDao goodsDao;



    public List<GoodsVo> listGoodsVo(){
        return goodsDao.listGoodsVo();
    }

    public GoodsVo getGoodsVoById(Long goodsId) {
        return goodsDao.getGoodsVoByGoodsId(goodsId);
    }

    /**
     * 减少库存
     * @param goodsVo
     */
    public int reduceStock(GoodsVo goodsVo) {
        MiaoshaGoods miaoshaGoods = new MiaoshaGoods();
        miaoshaGoods.setGoodsId(goodsVo.getId());
        return goodsDao.reduceStock(miaoshaGoods);
    }
}
