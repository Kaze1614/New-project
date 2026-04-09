INSERT INTO chapters(id, parent_id, title, sort_order)
VALUES (1, NULL, '必修一', 1)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  title = VALUES(title),
  sort_order = VALUES(sort_order);

INSERT INTO chapters(id, parent_id, title, sort_order)
VALUES (2, 1, '集合与函数概念', 10)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  title = VALUES(title),
  sort_order = VALUES(sort_order);

INSERT INTO chapters(id, parent_id, title, sort_order)
VALUES (3, 1, '基本初等函数', 20)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  title = VALUES(title),
  sort_order = VALUES(sort_order);

INSERT INTO chapters(id, parent_id, title, sort_order)
VALUES (4, 2, '函数及其表示', 11)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  title = VALUES(title),
  sort_order = VALUES(sort_order);

INSERT INTO chapters(id, parent_id, title, sort_order)
VALUES (5, 2, '函数的基本性质', 12)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  title = VALUES(title),
  sort_order = VALUES(sort_order);

INSERT INTO chapters(id, parent_id, title, sort_order)
VALUES (6, 3, '指数函数', 21)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  title = VALUES(title),
  sort_order = VALUES(sort_order);

INSERT INTO chapters(id, parent_id, title, sort_order)
VALUES (7, 3, '对数函数', 22)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  title = VALUES(title),
  sort_order = VALUES(sort_order);

INSERT INTO chapters(id, parent_id, title, sort_order)
VALUES (8, 3, '幂函数', 23)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  title = VALUES(title),
  sort_order = VALUES(sort_order);

INSERT INTO questions(id, chapter_id, title, content)
VALUES (1, 4, '函数极限基础题', '求 lim(x→1) (x^2-1)/(x-1) 的值，并说明步骤。')
ON DUPLICATE KEY UPDATE
  chapter_id = VALUES(chapter_id),
  title = VALUES(title),
  content = VALUES(content);

INSERT INTO questions(id, chapter_id, title, content)
VALUES (2, 7, '导数计算练习', '已知 f(x)=x^3-2x+1，求 f''(x) 并讨论单调性。')
ON DUPLICATE KEY UPDATE
  chapter_id = VALUES(chapter_id),
  title = VALUES(title),
  content = VALUES(content);

INSERT INTO questions(id, chapter_id, title, content)
VALUES (3, 6, '导数定义题', '用导数定义求 f(x)=x^2 在 x=2 处的导数。')
ON DUPLICATE KEY UPDATE
  chapter_id = VALUES(chapter_id),
  title = VALUES(title),
  content = VALUES(content);
