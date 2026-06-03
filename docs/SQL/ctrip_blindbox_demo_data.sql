-- 盲盒模块 - 示例数据
-- 表：blind_box_template / blind_box_order / blind_box_result / blind_box_preference
-- 关联用户：users (id=1~5), 关联目的地：destinations (id=1~5)

-- ----------------------------
-- 1. blind_box_template 表（5条：3条日常 + 2条限定）
-- ----------------------------
INSERT INTO blind_box_template (name, type, price, stock, rule_config, status) VALUES
('日常旅行盲盒', 0, 99.00, -1, '{"description": "日常随机旅行体验，99元解锁你的未知旅程"}', 0),
('周末微旅行盲盒', 0, 59.00, -1, '{"description": "周末出发，周边城市随机盲盒，说走就走的短途旅行"}', 0),
('豪华旅行盲盒', 0, 599.00, -1, '{"description": "高端酒店+精品路线，豪华旅行随机体验"}', 0),
('夏日海岛限定盲盒', 1, 199.00, 50, '{"activityStartTime": "2026-07-01T00:00:00", "activityEndTime": "2026-07-07T23:59:59", "discount": 0.8, "description": "夏日限定海岛盲盒，8折抢鲜体验"}', 0),
('国庆黄金周边盲盒', 1, 299.00, 30, '{"activityStartTime": "2026-10-01T00:00:00", "activityEndTime": "2026-10-07T23:59:59", "discount": 0.9, "description": "国庆黄金周周边游盲盒，限量抢购"}', 1);

-- ----------------------------
-- 2. blind_box_order 表（8条：覆盖各种状态）
-- ----------------------------
INSERT INTO blind_box_order (user_id, template_id, order_no, status, pay_amount, pay_method, pay_time, expire_at, created_at) VALUES
-- 张三：已开盒订单
(1, 1, 'BB202606010001', 2, 99.00, 'ALIPAY', '2026-06-01 10:05:00', '2026-06-01 10:20:00', '2026-06-01 10:05:00'),
-- 张三：待支付订单
(1, 4, 'BB202606020001', 0, 159.20, NULL, NULL, '2026-06-02 11:15:00', '2026-06-02 11:00:00'),
-- 李四：已开盒订单
(2, 2, 'BB202606010002', 2, 59.00, 'WECHAT_PAY', '2026-06-01 14:35:00', '2026-06-01 14:50:00', '2026-06-01 14:35:00'),
-- 李四：已取消订单（超时）
(2, 1, 'BB202605310001', 4, 99.00, NULL, NULL, '2026-05-31 09:15:00', '2026-05-31 09:00:00'),
-- 王五：已支付待开盒
(3, 3, 'BB202606020003', 1, 599.00, 'ALIPAY', '2026-06-02 16:10:00', '2026-06-02 16:25:00', '2026-06-02 16:10:00'),
-- 赵六：已开盒订单（豪华盲盒）
(4, 3, 'BB202606010004', 2, 599.00, 'ALIPAY', '2026-06-01 20:05:00', '2026-06-01 20:20:00', '2026-06-01 20:05:00'),
-- 孙七：已开盒订单（限定盲盒）
(5, 4, 'BB202606020005', 2, 159.20, 'WECHAT_PAY', '2026-06-02 09:10:00', '2026-06-02 09:25:00', '2026-06-02 09:10:00'),
-- 王五：已退款订单
(3, 4, 'BB202605310003', 3, 159.20, 'ALIPAY', '2026-05-31 15:20:00', '2026-05-31 15:35:00', '2026-05-31 15:20:00');

-- ----------------------------
-- 3. blind_box_result 表（5条：对应5个已开盒订单）
-- ----------------------------
INSERT INTO blind_box_result (order_id, destination, destination_id, theme, result_text, share_code, share_expires_at, opened_at) VALUES
-- 张三的盲盒结果：成都美食之旅
(1, '成都', 3, '美食',
'{
  "destination": "成都",
  "destinationId": 3,
  "theme": "美食",
  "budgetLevel": "STANDARD",
  "days": 4,
  "itinerary": {
    "day1": { "title": "抵达成都", "items": ["入住春熙路附近酒店", "晚上逛春熙路太古里", "品尝小龙坎火锅"] },
    "day2": { "title": "宽窄巷子-锦里", "items": ["上午游览宽窄巷子", "中午品尝蛋烘糕、钟水饺", "下午逛锦里古街", "晚上看川剧变脸"] },
    "day3": { "title": "都江堰-青城山", "items": ["上午前往都江堰", "下午游览青城山", "晚上返回市区"] },
    "day4": { "title": "大熊猫基地-返程", "items": ["上午参观大熊猫繁育研究基地", "下午根据航班返程"] }
  },
  "hotel": { "name": "成都春熙路亚朵酒店", "address": "成都市锦江区春熙路99号", "rating": 4.5 },
  "transport": { "type": "高铁", "departureTime": "G8682 08:30-12:15" },
  "budget": { "total": 2580, "transport": 600, "hotel": 1200, "food": 500, "tickets": 280 }
}',
'aB3xYz', '2026-06-08 10:30:00', '2026-06-01 10:30:00'),

-- 李四的盲盒结果：杭州周末微旅行
(3, '杭州', 1, '休闲',
'{
  "destination": "杭州",
  "destinationId": 1,
  "theme": "休闲",
  "budgetLevel": "ECONOMY",
  "days": 2,
  "itinerary": {
    "day1": { "title": "西湖环游", "items": ["上午从断桥出发沿白堤步行", "中午在楼外楼品尝西湖醋鱼", "下午乘船游湖登三潭印月", "傍晚苏堤漫步看雷峰夕照"] },
    "day2": { "title": "灵隐寺-龙井茶村", "items": ["上午游览灵隐寺飞来峰", "中午在天竺路品尝素斋", "下午前往龙井村品茶", "下午返程"] }
  },
  "hotel": { "name": "杭州西湖边青年旅舍", "address": "杭州市西湖区虎跑路15号", "rating": 4.0 },
  "transport": { "type": "高铁", "departureTime": "G7591 07:45-09:30" },
  "budget": { "total": 860, "transport": 300, "hotel": 260, "food": 200, "tickets": 100 }
}',
'kM8pQr', '2026-06-08 15:00:00', '2026-06-01 15:00:00'),

-- 赵六的盲盒结果：巴厘岛豪华之旅
(6, '巴厘岛', 5, '度假',
'{
  "destination": "巴厘岛",
  "destinationId": 5,
  "theme": "度假",
  "budgetLevel": "LUXURY",
  "days": 5,
  "itinerary": {
    "day1": { "title": "抵达巴厘岛", "items": ["入住金巴兰海滩五星级度假村", "傍晚海滩BBQ晚餐看日落"] },
    "day2": { "title": "乌布文化之旅", "items": ["德格拉朗梯田", "乌布皇宫", "乌布传统市场", "晚上体验乌布特色餐厅"] },
    "day3": { "title": "南湾水上活动", "items": ["上午浮潜和水上飞伞", "下午前往海神庙看日落", "晚上金巴兰海鲜大餐"] },
    "day4": { "title": "蓝梦岛一日游", "items": ["浮潜看Manta Ray", "漫步梦幻海滩", "悬崖秋千打卡"] },
    "day5": { "title": "SPA体验-返程", "items": ["上午享受巴厘岛传统SPA", "下午前往机场返程"] }
  },
  "hotel": { "name": "巴厘岛金巴兰湾四季度假村", "address": "Jimbaran Bay, Bali, Indonesia", "rating": 4.9 },
  "transport": { "type": "飞机", "departureTime": "MU5057 10:20-16:45" },
  "budget": { "total": 12800, "transport": 5000, "hotel": 5500, "food": 1500, "tickets": 800 }
}',
'nX2vLw', '2026-06-08 20:30:00', '2026-06-01 20:30:00'),

-- 孙七的盲盒结果：京都文化之旅
(7, '京都', 4, '文化',
'{
  "destination": "京都",
  "destinationId": 4,
  "theme": "文化",
  "budgetLevel": "STANDARD",
  "days": 4,
  "itinerary": {
    "day1": { "title": "抵达京都", "items": ["入住祇园附近酒店", "傍晚漫步花见小路", "晚餐品尝京都怀石料理"] },
    "day2": { "title": "伏见稻荷大社-清水寺", "items": ["清晨前往伏见稻荷大社千本鸟居", "下午游览清水寺和高台寺", "晚上逛祇园商店街"] },
    "day3": { "title": "金阁寺-岚山", "items": ["上午参观金阁寺", "下午游览岚山竹林小径", "体验渡月桥畔和服拍照"] },
    "day4": { "title": "奈良半日游-返程", "items": ["上午前往奈良公园喂小鹿", "下午根据航班/高铁返程"] }
  },
  "hotel": { "name": "京都祇园赛莱斯廷酒店", "address": "京都府京都市东山区祇园町南侧", "rating": 4.6 },
  "transport": { "type": "飞机", "departureTime": "HO1335 09:10-13:30" },
  "budget": { "total": 6500, "transport": 3200, "hotel": 2000, "food": 800, "tickets": 500 }
}',
'pT5jKd', '2026-06-09 09:40:00', '2026-06-02 09:40:00'),

-- 张三的第二个盲盒结果：北京文化之旅
(2, '北京', 2, '文化',
'{
  "destination": "北京",
  "destinationId": 2,
  "theme": "文化",
  "budgetLevel": "STANDARD",
  "days": 3,
  "itinerary": {
    "day1": { "title": "故宫-景山公园", "items": ["上午游览故宫博物院", "中午在四季民福烤鸭店用餐", "下午登景山公园俯瞰故宫全景", "晚上逛王府井大街"] },
    "day2": { "title": "八达岭长城", "items": ["上午前往八达岭长城", "中午在长城脚下用餐", "下午返回市区游览奥林匹克公园", "晚上鸟巢水立方夜景"] },
    "day3": { "title": "颐和园-天坛-返程", "items": ["上午游览颐和园昆明湖", "下午参观天坛祈年殿", "根据航班/高铁时间返程"] }
  },
  "hotel": { "name": "北京王府井亚朵酒店", "address": "北京市东城区王府井大街22号", "rating": 4.3 },
  "transport": { "type": "高铁", "departureTime": "G4 06:30-11:00" },
  "budget": { "total": 3200, "transport": 1200, "hotel": 900, "food": 600, "tickets": 500 }
}',
'qW7nRs', '2026-06-09 11:30:00', '2026-06-02 11:30:00');

-- ----------------------------
-- 4. blind_box_preference 表（8条：对应8个订单）
-- ----------------------------
INSERT INTO blind_box_preference (order_id, departure_city, budget_level, theme) VALUES
-- 张三 - 成都美食
(1, '上海', 1, '美食'),
-- 张三 - 待支付（夏日海岛限定）
(2, '上海', 1, '海岛'),
-- 李四 - 杭州休闲
(3, '南京', 0, '休闲'),
-- 李四 - 已取消
(4, '广州', 1, '古镇'),
-- 王五 - 豪华盲盒（已支付待开盒）
(5, '深圳', 2, '度假'),
-- 赵六 - 巴厘岛豪华
(6, '北京', 2, '度假'),
-- 孙七 - 京都文化
(7, '杭州', 1, '文化'),
-- 王五 - 已退款
(8, '深圳', 1, '美食');

-- ----------------------------
-- 清空测试数据
-- ----------------------------
-- DELETE FROM blind_box_preference;
-- DELETE FROM blind_box_result;
-- DELETE FROM blind_box_order;
-- DELETE FROM blind_box_template;

-- 重置自增主键
-- ALTER TABLE blind_box_preference AUTO_INCREMENT = 1;
-- ALTER TABLE blind_box_result AUTO_INCREMENT = 1;
-- ALTER TABLE blind_box_order AUTO_INCREMENT = 1;
-- ALTER TABLE blind_box_template AUTO_INCREMENT = 1;
