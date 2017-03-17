CREATE TABLE eps_item (
  code varchar(20) NOT NULL, -- 商品代号
  name varchar(32) NOT NULL,	-- 商品名称
  catalog tinyint NOT NULL,	-- 商品类别
  PRIMARY KEY (code)
)

CREATE TABLE eps_orders (
  order_id varchar(36) NOT NULL,		-- 订单编号
  merchant_id varchar(32) DEFAULT NULL,		-- 油站编码
  merchant_name varchar(64) DEFAULT NULL,		-- 油站名称
  generator varchar(32) DEFAULT NULL,			-- 订单生成设备（BPOS）
  order_time bigint NOT NULL,			-- 订单生成时间
  shift_number varchar(10) DEFAULT NULL,		-- 收银员班次号
  clerk_id varchar(16) DEFAULT NULL,			-- 收银员编号
  original_amount decimal(8,2) NOT NULL DEFAULT '0.00',		-- 应付金额
  payment_amount decimal(8,2) DEFAULT '0.00',					-- 实付金额
  coupon_amount decimal(8,2) DEFAULT '0.00',					-- 优惠券金额
  loyalty_point bigint DEFAULT NULL,					-- 积分
  status tinyint DEFAULT '0',							-- 订单状态（0，代支付；1，已支付；2，订单锁定）
  pay_type tinyint DEFAULT '0',						    -- 支付类型 （1：微信；2：支付宝；3：会员）
  card_number varchar(50) DEFAULT NULL						-- 支付卡号
  PRIMARY KEY (order_id)
)

CREATE TABLE eps_saleitems (
  id varchar(36) NOT NULL,							-- 销售商品明细编号
  product_code varchar(20) NOT NULL,					-- 销售商品编号
  unit_measure varchar(5) NOT NULL DEFAULT 'LTR',		-- 商品计量单位（LTR标识升）
  unit_price decimal(5,2) NOT NULL DEFAULT '0.00',	-- 商品单价
  quantity decimal(6,2) NOT NULL DEFAULT '0.00',		-- 商品金额
  item_seq tinyint NOT NULL,						-- 销售商品序号
  tax_code varchar(20) NOT NULL,						-- 税号
  order_id varchar(36) NOT NULL,						-- 销售商品所处订单编号
  amount decimal(6,2) NOT NULL DEFAULT '0.00',		-- 商品数量
  PRIMARY KEY (id),
  CONSTRAINT FKsaleItem846585 FOREIGN KEY (order_id) REFERENCES eps_orders (order_id),
  CONSTRAINT FKsaleItem855683 FOREIGN KEY (product_code) REFERENCES eps_item (code)
)