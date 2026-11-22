DROP TABLE IF EXISTS itinerary_item;
DROP TABLE IF EXISTS itinerary_day;
DROP TABLE IF EXISTS itinerary;
DROP TABLE IF EXISTS blind_box_result;
DROP TABLE IF EXISTS blind_box_order;

CREATE TABLE blind_box_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    template_id BIGINT,
    order_no VARCHAR(30),
    status TINYINT,
    pay_amount DECIMAL(10, 2),
    pay_method VARCHAR(20),
    pay_time TIMESTAMP,
    expire_at TIMESTAMP,
    refund_reason VARCHAR(500),
    refunded_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE blind_box_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    destination VARCHAR(100),
    destination_id BIGINT,
    theme VARCHAR(50),
    result_text CLOB,
    result_image_url VARCHAR(500),
    share_code VARCHAR(20),
    share_expires_at TIMESTAMP,
    opened_at TIMESTAMP,
    imported_itinerary_id BIGINT,
    created_at TIMESTAMP
);

CREATE TABLE itinerary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    title VARCHAR(100),
    destination_id BIGINT,
    start_date DATE,
    end_date DATE,
    status TINYINT,
    share_code VARCHAR(20),
    share_expires_at TIMESTAMP,
    view_count INT,
    like_count INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE itinerary_day (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    itinerary_id BIGINT,
    day_number INT,
    title VARCHAR(100),
    sort_order INT
);

CREATE TABLE itinerary_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    itinerary_day_id BIGINT,
    type TINYINT,
    name VARCHAR(5),
    location VARCHAR(255),
    time_slot VARCHAR(50),
    description CLOB,
    sort_order INT
);
