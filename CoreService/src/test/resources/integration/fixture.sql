INSERT INTO categories (category_id, category_name) VALUES (10, 'r03-category');
INSERT INTO hobbies (hobby_id, hobby_name) VALUES (20, 'r03-hobby');
INSERT INTO category_hobbies (category_id, hobby_id) VALUES (10, 20);

INSERT INTO Users (email, password_hash, name, nickname, login_method, social_provider, account_status, authority)
VALUES
  ('host@r03.example.test', 'fixture', 'R03 Host', 'r03-host', 'EMAIL', 'NONE', 'Active', 'USER'),
  ('member@r03.example.test', 'fixture', 'R03 Member', 'r03-member', 'EMAIL', 'NONE', 'Active', 'USER'),
  ('outsider@r03.example.test', 'fixture', 'R03 Outsider', 'r03-outsider', 'EMAIL', 'NONE', 'Active', 'USER');

INSERT INTO Products (id, product_code, title, description, price, email, category_id, hobby_id, transaction_type, registration_type, max_participants, current_participants, days)
VALUES (100, 'r03-product-100', 'R03 product', 'fixture product', 1000, 'host@r03.example.test', 10, 20, '대면', '구매', 2, 1, 'MON');
INSERT INTO productrequests (id, product_id, requester_email) VALUES (200, 100, 'member@r03.example.test');
INSERT INTO chatrooms (chatroom_id, chatname, product_id, request_email) VALUES (300, 'R03 chat', 100, 'member@r03.example.test');
INSERT INTO boards (id, name, description, host_email, status) VALUES (400, 'R03 board', 'fixture board', 'host@r03.example.test', 'ACTIVE');
INSERT INTO board_members (board_id, user_email, role, status) VALUES (400, 'member@r03.example.test', 'MEMBER', 'ACTIVE');
