package com.twq.miaosha.vo;

import com.twq.miaosha.domain.Goods;

import java.util.Date;

/**
 * 联合查找的实体类，用于对应dao中联合查找的结果
 *包括 goods中所有字段 和 Miaoshagoods中的部分字段
 * 因此 继承了 Goods 这里面就包含了 Goods中的字段
 */
public class GoodsVo extends Goods {
	private Double miaoshaPrice;
	private Integer stockCount;
	private Date startDate;
	private Date endDate;
	public Integer getStockCount() {
		return stockCount;
	}
	public void setStockCount(Integer stockCount) {
		this.stockCount = stockCount;
	}
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	public Double getMiaoshaPrice() {
		return miaoshaPrice;
	}
	public void setMiaoshaPrice(Double miaoshaPrice) {
		this.miaoshaPrice = miaoshaPrice;
	}
}
