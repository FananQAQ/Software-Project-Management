-- 初始化测试数据，INSERT IGNORE 防止重启时重复插入
INSERT IGNORE INTO flights (flight_no, origin, destination, latitude, longitude, altitude, speed, heading, status, updated_at) VALUES
('CA1001', '北京首都', '上海浦东', 35.50, 118.00, 10000, 850, 135, 'IN_FLIGHT', NOW()),
('MU2201', '上海浦东', '广州白云', 28.00, 117.50,  9500, 820, 210, 'IN_FLIGHT', NOW()),
('CZ3301', '广州白云', '北京首都', 32.00, 114.00, 10500, 870,  10, 'IN_FLIGHT', NOW()),
('HU7701', '海口美兰', '北京首都', 26.00, 112.00,  9000, 800,  15, 'IN_FLIGHT', NOW()),
('3U8801', '成都天府', '上海浦东', 30.50, 110.00, 10000, 840,  95, 'IN_FLIGHT', NOW());
