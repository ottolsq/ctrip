-- 内容与目的地模块 - 示例数据
-- 表：destinations / attractions / guides / comments

-- ----------------------------
-- 1. destinations 表（5条）
-- ----------------------------
INSERT INTO destinations (name, country, province, description, best_season, cover_url, image_urls) VALUES
('杭州', '中国', '浙江省', '杭州位于中国东南沿海，浙江省北部，以其秀丽的西湖风光和深厚的文化底蕴闻名于世，素有"人间天堂"之美誉。', 1, 'https://img.example.com/destinations/hangzhou.jpg', '["https://img.example.com/destinations/hangzhou-1.jpg", "https://img.example.com/destinations/hangzhou-2.jpg", "https://img.example.com/destinations/hangzhou-3.jpg"]'),
('北京', '中国', '北京市', '中国首都，拥有三千多年建城史，是政治、文化、国际交往和科技创新中心。故宫、长城、颐和园等世界文化遗产享誉全球。', 3, 'https://img.example.com/destinations/beijing.jpg', '["https://img.example.com/destinations/beijing-1.jpg", "https://img.example.com/destinations/beijing-2.jpg", "https://img.example.com/destinations/beijing-3.jpg"]'),
('成都', '中国', '四川省', '成都，天府之国，以大熊猫繁育研究基地、都江堰、青城山和麻辣火锅闻名。悠闲的生活节奏和丰富的美食文化吸引无数游客。', 2, 'https://img.example.com/destinations/chengdu.jpg', '["https://img.example.com/destinations/chengdu-1.jpg", "https://img.example.com/destinations/chengdu-2.jpg"]'),
('京都', '日本', '关西地区', '日本古都，拥有17处世界文化遗产。金阁寺、伏见稻荷大社、岚山竹林等景点展现了日本传统美学的精髓。', 4, 'https://img.example.com/destinations/kyoto.jpg', '["https://img.example.com/destinations/kyoto-1.jpg", "https://img.example.com/destinations/kyoto-2.jpg", "https://img.example.com/destinations/kyoto-3.jpg"]'),
('巴厘岛', '印度尼西亚', '巴厘省', '印度尼西亚著名度假胜地，以美丽的海滩、梯田、寺庙和独特的巴厘文化著称。是蜜月旅行和冲浪爱好者的天堂。', 5, 'https://img.example.com/destinations/bali.jpg', '["https://img.example.com/destinations/bali-1.jpg", "https://img.example.com/destinations/bali-2.jpg"]');

-- ----------------------------
-- 2. attractions 表（12条，关联上述目的地）
-- ----------------------------
-- 杭州景点 (destination_id = 1)
INSERT INTO attractions (destination_id, name, description, location, ticket_price, cover_url, image_urls) VALUES
(1, '西湖', '西湖是中国著名的文化景观，以"西湖十景"闻名于世。苏堤春晓、断桥残雪、雷峰夕照等景观自古流传。', '浙江省杭州市西湖区龙井路1号', 0.00, 'https://img.example.com/attractions/west-lake.jpg', '["https://img.example.com/attractions/west-lake-1.jpg", "https://img.example.com/attractions/west-lake-2.jpg"]'),
(1, '灵隐寺', '灵隐寺始建于东晋咸和元年（公元326年），是江南著名佛教寺院。寺前飞来峰石刻造像是中国南方石窟艺术的重要遗存。', '浙江省杭州市西湖区灵隐路法云弄1号', 75.00, 'https://img.example.com/attractions/lingyin-temple.jpg', '["https://img.example.com/attractions/lingyin-temple-1.jpg"]'),
(1, '宋城', '宋城是中国最大的宋文化主题公园，以《宋城千古情》演出闻名，再现了北宋都城汴京的繁华景象。', '浙江省杭州市西湖区之江路148号', 320.00, 'https://img.example.com/attractions/songcheng.jpg', NULL);

-- 北京景点 (destination_id = 2)
INSERT INTO attractions (destination_id, name, description, location, ticket_price, cover_url, image_urls) VALUES
(2, '故宫博物院', '故宫又称紫禁城，是明清两代皇宫，占地72万平方米，是世界上现存规模最大的木质结构古建筑群。', '北京市东城区景山前街4号', 60.00, 'https://img.example.com/attractions/forbidden-city.jpg', '["https://img.example.com/attractions/forbidden-city-1.jpg", "https://img.example.com/attractions/forbidden-city-2.jpg", "https://img.example.com/attractions/forbidden-city-3.jpg"]'),
(2, '八达岭长城', '八达岭长城是万里长城的重要组成部分，海拔最高、保存最完整的一段，有"不到长城非好汉"之说。', '北京市延庆区G6京藏高速58号出口', 40.00, 'https://img.example.com/attractions/great-wall.jpg', '["https://img.example.com/attractions/great-wall-1.jpg"]'),
(2, '颐和园', '颐和园是中国古典园林之首，以昆明湖、万寿山为基址，以杭州西湖为蓝本建造的大型皇家园林。', '北京市海淀区新建宫门路19号', 30.00, 'https://img.example.com/attractions/summer-palace.jpg', NULL);

-- 成都景点 (destination_id = 3)
INSERT INTO attractions (destination_id, name, description, location, ticket_price, cover_url, image_urls) VALUES
(3, '大熊猫繁育研究基地', '成都大熊猫繁育研究基地是世界著名的大熊猫迁地保护基地，可以近距离观赏不同年龄段的大熊猫。', '四川省成都市成华区熊猫大道1375号', 55.00, 'https://img.example.com/attractions/panda-base.jpg', '["https://img.example.com/attractions/panda-base-1.jpg", "https://img.example.com/attractions/panda-base-2.jpg"]'),
(3, '都江堰', '都江堰是战国时期秦国蜀郡太守李冰主持建造的大型水利工程，两千多年来一直发挥着防洪灌溉作用。', '四川省成都市都江堰市公园路', 80.00, 'https://img.example.com/attractions/dujiangyan.jpg', NULL),
(3, '宽窄巷子', '宽窄巷子是成都遗留下来的较成规模的清朝古街道，由宽巷子、窄巷子和井巷子组成，是成都老城墙的遗存。', '四川省成都市青羊区长顺上街127号', 0.00, 'https://img.example.com/attractions/kuanzhai.jpg', '["https://img.example.com/attractions/kuanzhai-1.jpg"]');

-- 京都景点 (destination_id = 4)
INSERT INTO attractions (destination_id, name, description, location, ticket_price, cover_url, image_urls) VALUES
(4, '伏见稻荷大社', '伏见稻荷大社是日本全国约三万座稻荷神社的总社，以千本鸟居闻名于世，朱红色鸟居沿山而建的景象极为壮观。', '京都府京都市伏见区深谷薮之内町68', 0.00, 'https://img.example.com/attractions/fushimi-inari.jpg', '["https://img.example.com/attractions/fushimi-inari-1.jpg", "https://img.example.com/attractions/fushimi-inari-2.jpg"]'),
(4, '金阁寺', '金阁寺正式名称为鹿苑寺，因寺内舍利殿外墙以金箔装饰而得名，倒映在镜湖池中的金色阁殿是京都的标志性景观。', '京都府京都市北区金阁寺町1', 400.00, 'https://img.example.com/attractions/kinkaku-ji.jpg', NULL);

-- ----------------------------
-- 3. guides 表（6条攻略，关联目的地和用户）
-- ----------------------------
INSERT INTO guides (author_id, title, content, destination_id, cover_url, image_urls, status, view_count, like_count) VALUES
(1, '杭州三日游完全攻略 — 西湖、灵隐、宋城一次玩够', '# 杭州三日游完全攻略\n\n## Day 1: 西湖环游\n早上从断桥出发，沿白堤步行至孤山，参观浙江省博物馆。中午在楼外楼品尝西湖醋鱼和龙井虾仁。下午乘船游湖，登三潭印月岛。傍晚漫步苏堤，欣赏雷峰夕照。\n\n## Day 2: 灵隐寺与龙井\n上午游览灵隐寺和飞来峰石刻，感受千年佛教文化。中午在天竺路品尝素斋。下午前往龙井村，品茶赏景，体验茶文化。\n\n## Day 3: 宋城体验\n全天游览宋城主题公园，观看《宋城千古情》大型演出，体验宋代市井生活，穿宋装、品宋宴。\n\n## 实用贴士\n- 交通：杭州市内地铁+公交很方便\n- 住宿：推荐西湖附近，步行即可到达主要景点\n- 最佳季节：春季3-5月，秋季9-11月', 1, 'https://img.example.com/guides/hangzhou-3day.jpg', '["https://img.example.com/guides/hangzhou-3day-1.jpg", "https://img.example.com/guides/hangzhou-3day-2.jpg"]', 1, 1523, 89),
(1, '北京必打卡景点TOP5 — 故宫长城一个都不能少', '# 北京必打卡景点TOP5\n\n## 1. 故宫博物院\n建议提前在官网预约门票，游览路线：午门→太和门→太和殿→乾清宫→御花园→神武门。全程约3小时。\n\n## 2. 八达岭长城\n推荐乘坐缆车上山，节省体力。最佳拍摄点在北八楼。注意防晒和带足饮用水。\n\n## 3. 颐和园\n昆明湖划船是必体验项目，长廊彩绘精美绝伦，值得细细品味。\n\n## 4. 天坛公园\n祈年殿是标志性建筑，建议早上去，光线最好。\n\n## 5. 南锣鼓巷\n老北京胡同文化代表，各种特色小吃和文创店铺。', 2, 'https://img.example.com/guides/beijing-top5.jpg', NULL, 1, 2341, 156),
(2, '成都美食之旅 — 火锅、串串、串串香吃到停不下来', '# 成都美食之旅\n\n## 火锅推荐\n- **小龙坎**：经典川味火锅，毛肚和鹅肠是必点\n- **大龙燚**：辣度可选，适合不太能吃辣的朋友\n- **蜀大侠**：特色菜"花千骨"值得尝试\n\n## 小吃必吃\n1. 锦里古街的三大炮\n2. 宽窄巷子的蛋烘糕\n3. 春熙路的钟水饺\n4. 建设路的烤脑花\n\n## 串串推荐\n马路边边串串香和冒椒火辣是本地人最爱。\n\n## 温馨提示\n成都美食普遍偏辣，肠胃敏感的朋友请备好肠胃药。', 3, 'https://img.example.com/guides/chengdu-food.jpg', '["https://img.example.com/guides/chengdu-food-1.jpg", "https://img.example.com/guides/chengdu-food-2.jpg", "https://img.example.com/guides/chengdu-food-3.jpg"]', 1, 3876, 234),
(2, '京都赏枫完全指南 — 红叶季最佳观赏时间和路线', '# 京都赏枫完全指南\n\n## 最佳时间\n每年11月中旬至12月初是京都红叶最佳观赏期。\n\n## 推荐路线\n\n### 岚山区域（半天）\n天龙寺→竹林小径→常寂光寺→大河内山庄\n\n### 东山区域（一天）\n清水寺→高台寺→永观堂→南禅寺\n\n### 北部区域（一天）\n金阁寺→龙安寺→妙心寺→仁和寺\n\n## 注意事项\n- 红叶季游客非常多，建议早上7点前到达热门景点\n- 夜间点灯活动期间别有一番风味\n- 带一件厚外套，11月的京都早晚很冷', 4, 'https://img.example.com/guides/kyoto-autumn.jpg', '["https://img.example.com/guides/kyoto-autumn-1.jpg"]', 1, 987, 67),
(5, '巴厘岛自由行 — 五天四夜蜜月之旅', '# 巴厘岛自由行：五天四夜蜜月之旅\n\n## Day 1: 抵达 & 库塔海滩\n入住库塔海边酒店，傍晚在库塔海滩看日落，晚餐选择海滩边的BBQ餐厅。\n\n## Day 2: 乌布文化之旅\n德格拉朗梯田→乌布皇宫→乌布传统市场→象窟遗址。\n\n## Day 3: 南湾水上活动\n浮潜、香蕉船、水上飞伞，下午前往海神庙看日落。\n\n## Day 4: 蓝梦岛一日游\n参加一日游，浮潜看Manta Ray，漫步梦幻海滩。\n\n## Day 5: SPA & 返程\n上午体验巴厘岛传统SPA，下午前往机场。\n\n## 预算参考\n两人五天四夜约 8000-12000 元（含机票酒店）。', 5, 'https://img.example.com/guides/bali-honeymoon.jpg', '["https://img.example.com/guides/bali-honeymoon-1.jpg", "https://img.example.com/guides/bali-honeymoon-2.jpg"]', 1, 1245, 78),
(3, '2026年成都看大熊猫最全攻略', '# 2026年成都看大熊猫最全攻略\n\n## 基本信息\n- **地址**：成都市成华区熊猫大道1375号\n- **开放时间**：7:30-18:00（全年开放）\n- **门票**：55元/人\n- **建议游览时间**：3-4小时\n\n## 最佳到访时间\n- 大熊猫怕热不怕冷，上午9-10点最活跃\n- 避开夏季高温和节假日人流高峰\n- 冬季（12-2月）可以看到熊猫在雪地里玩耍\n\n## 游览路线\n太阳产房→月亮产房→幼年大熊猫活动场→成年大熊猫活动场→小熊猫活动场\n\n## 注意事项\n1. 保持安静，不要拍打玻璃\n2. 禁止使用闪光灯拍照\n3. 不要向熊猫投掷任何物品', 3, 'https://img.example.com/guides/panda-guide.jpg', NULL, 0, 128, 12);

-- ----------------------------
-- 4. comments 表（15条评论，含嵌套回复）
-- ----------------------------
-- 杭州三日游攻略的评论 (guide_id = 1)
INSERT INTO comments (guide_id, user_id, parent_id, content, like_count) VALUES
(1, 2, NULL, '攻略写得太详细了！上个月刚去过杭州，完全按照这个路线玩的，非常棒！', 12),
(1, 3, 1, '请问西湖环游大概需要多长时间呀？', 3),
(1, 2, 3, '走马观花的话半天就够，想慢慢欣赏的话建议一整天', 5),
(1, 5, NULL, '宋城的千古情演出真的很震撼，强烈推荐！', 8);

-- 北京必打卡攻略的评论 (guide_id = 2)
INSERT INTO comments (guide_id, user_id, parent_id, content, like_count) VALUES
(2, 3, NULL, '故宫建议至少留一整天，根本逛不完！', 15),
(2, 5, NULL, '长城周末人太多了，工作日去体验好很多', 7),
(2, 1, 5, '是的，我上次周末去的北八楼排队拍照花了半个小时', 4),
(2, 4, NULL, '天坛的回音壁真的很神奇，带孩子去一定要体验一下', 6);

-- 成都美食攻略的评论 (guide_id = 3)
INSERT INTO comments (guide_id, user_id, parent_id, content, like_count) VALUES
(3, 1, NULL, '作为成都人表示这家小龙坎确实正宗！推荐微辣就好', 20),
(3, 5, NULL, '建设路的烤脑花真的是我的最爱，每次去必吃！', 11),
(3, 4, 9, '不太能吃辣的朋友可以从鸳鸯锅开始尝试', 6);

-- 京都赏枫攻略的评论 (guide_id = 4)
INSERT INTO comments (guide_id, user_id, parent_id, content, like_count) VALUES
(4, 1, NULL, '永观堂的夜间点灯真的太美了，今年还要去！', 9),
(4, 3, NULL, '请问从大阪到京都坐JR大概多长时间？', 4),
(4, 2, 12, 'JR新快速大概30分钟就到京都站了，很方便', 7);
