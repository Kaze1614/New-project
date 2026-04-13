INSERT INTO chapters(id, parent_id, title, sort_order)
VALUES (1, NULL, '必修一', 1)
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), title = VALUES(title), sort_order = VALUES(sort_order);

INSERT INTO chapters(id, parent_id, title, sort_order)
VALUES (2, 1, '集合与函数概念', 10)
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), title = VALUES(title), sort_order = VALUES(sort_order);

INSERT INTO chapters(id, parent_id, title, sort_order)
VALUES (3, 1, '基本初等函数', 20)
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), title = VALUES(title), sort_order = VALUES(sort_order);

INSERT INTO chapters(id, parent_id, title, sort_order)
VALUES (4, 2, '函数及其表示', 11)
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), title = VALUES(title), sort_order = VALUES(sort_order);

INSERT INTO chapters(id, parent_id, title, sort_order)
VALUES (5, 2, '函数的基本性质', 12)
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), title = VALUES(title), sort_order = VALUES(sort_order);

INSERT INTO chapters(id, parent_id, title, sort_order)
VALUES (6, 3, '指数函数', 21)
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), title = VALUES(title), sort_order = VALUES(sort_order);

INSERT INTO questions(id, chapter_id, title, content, type, options_json, answer_json, explanation, difficulty) VALUES
(1, 4, '函数定义域判定1', '函数 f(x)=1/(x-2) 的定义域是？', 'SINGLE', '["A. x>2","B. x<2","C. x≠2","D. x≥2"]', '["C"]', '分母不能为0，所以 x-2≠0，得到 x≠2。', 'EASY'),
(2, 4, '函数定义域判定2', '函数 y=√(x-1) 的定义域是？', 'SINGLE', '["A. x≥1","B. x>1","C. x≤1","D. x<1"]', '["A"]', '根号内需非负：x-1≥0，得 x≥1。', 'EASY'),
(3, 4, '函数值计算1', '已知 f(x)=2x+1，则 f(3)=？', 'FILL', NULL, '["7"]', '代入 x=3，得到 f(3)=7。', 'EASY'),
(4, 4, '函数值计算2', '已知 f(x)=x^2-1，则 f(-2)=？', 'FILL', NULL, '["3"]', '(-2)^2-1=4-1=3。', 'EASY'),
(5, 4, '函数表示法', '下列属于函数关系的是？', 'SINGLE', '["A. 圆 x^2+y^2=1","B. y=x+1","C. y^2=x","D. x=y^2"]', '["B"]', '对每个 x，y=x+1 唯一确定。', 'MEDIUM'),
(6, 5, '单调性基础1', '函数 y=3x 在 R 上是？', 'SINGLE', '["A. 单调递增","B. 单调递减","C. 先增后减","D. 非单调"]', '["A"]', '一次函数斜率3>0，在R上单调递增。', 'EASY'),
(7, 5, '单调性基础2', '函数 y=-2x 在 R 上是？', 'SINGLE', '["A. 单调递增","B. 单调递减","C. 常函数","D. 周期函数"]', '["B"]', '斜率-2<0，在R上单调递减。', 'EASY'),
(8, 5, '奇偶性判定1', '函数 f(x)=x^2 的奇偶性是？', 'SINGLE', '["A. 奇函数","B. 偶函数","C. 非奇非偶","D. 周期函数"]', '["B"]', 'f(-x)=(-x)^2=x^2=f(x)。', 'MEDIUM'),
(9, 5, '奇偶性判定2', '函数 f(x)=x^3 的奇偶性是？', 'SINGLE', '["A. 奇函数","B. 偶函数","C. 非奇非偶","D. 常函数"]', '["A"]', 'f(-x)=(-x)^3=-x^3=-f(x)。', 'MEDIUM'),
(10, 5, '最值判断', '函数 y=-(x-1)^2+4 的最大值是？', 'FILL', NULL, '["4"]', '开口向下抛物线，顶点纵坐标4为最大值。', 'MEDIUM'),
(11, 5, '零点个数', '函数 y=x^2-1 在区间[-2,2]内零点个数为？', 'FILL', NULL, '["2"]', 'x^2-1=0 得 x=±1，共2个零点。', 'MEDIUM'),
(12, 6, '指数运算1', '2^3 * 2^4 = ?', 'FILL', NULL, '["128"]', '同底数幂相乘指数相加：2^(3+4)=2^7=128。', 'EASY'),
(13, 6, '指数运算2', '(3^2)^3 = ?', 'FILL', NULL, '["729"]', '幂的乘方指数相乘：3^(2*3)=3^6=729。', 'MEDIUM'),
(14, 6, '指数函数图像1', 'y=2^x 的图像经过哪一点？', 'SINGLE', '["A. (0,1)","B. (1,0)","C. (-1,2)","D. (2,0)"]', '["A"]', '任意 a^x 在 x=0 时都等于1。', 'EASY'),
(15, 6, '指数函数图像2', '函数 y=(1/2)^x 的单调性是？', 'SINGLE', '["A. 单调递增","B. 单调递减","C. 先增后减","D. 常函数"]', '["B"]', '底数 0<a<1 时指数函数单调递减。', 'MEDIUM'),
(16, 6, '指数不等式', '若 2^x > 8，则 x 的取值范围为？', 'SINGLE', '["A. x>3","B. x<3","C. x≥3","D. x≤3"]', '["A"]', '2^x>2^3 且底数大于1，故 x>3。', 'MEDIUM'),
(17, 4, '函数解析式', '已知 f(x+1)=2x+3，则 f(x)=？', 'SINGLE', '["A. 2x+1","B. 2x+3","C. 2x-1","D. x+2"]', '["A"]', '令 t=x+1，则 f(t)=2(t-1)+3=2t+1，所以 f(x)=2x+1。', 'HARD'),
(18, 5, '函数对称性', '函数 y=|x| 关于哪条轴对称？', 'SINGLE', '["A. x轴","B. y轴","C. 直线y=x","D. 原点"]', '["B"]', '绝对值函数图像关于y轴对称。', 'EASY'),
(19, 5, '复合函数', '设 f(x)=2x, g(x)=x+1，则 (f∘g)(2)=？', 'FILL', NULL, '["6"]', '先算 g(2)=3，再算 f(3)=6。', 'MEDIUM'),
(20, 6, '指数比较', '比较 2^0.5 与 2^0.6 的大小关系。', 'SINGLE', '["A. 前者大","B. 后者大","C. 相等","D. 无法比较"]', '["B"]', '底数2>1，指数越大函数值越大。', 'EASY')
ON DUPLICATE KEY UPDATE
chapter_id = VALUES(chapter_id),
title = VALUES(title),
content = VALUES(content),
type = VALUES(type),
options_json = VALUES(options_json),
answer_json = VALUES(answer_json),
explanation = VALUES(explanation),
difficulty = VALUES(difficulty);
