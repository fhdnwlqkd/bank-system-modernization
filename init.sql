-- 1. 시퀀스 관리 테이블 생성
CREATE TABLE IF NOT EXISTS sys_sequences (
    sequence_name VARCHAR(50) NOT NULL PRIMARY KEY,
    current_value BIGINT NOT NULL DEFAULT 0,
    increment_by INT NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- 2. 초기 시퀀스 데이터 삽입
INSERT IGNORE INTO sys_sequences (sequence_name, current_value, increment_by)
VALUES ('card_num_seq', 101, 1);

INSERT IGNORE INTO sys_sequences (sequence_name, current_value, increment_by)
VALUES ('account_num_seq', 1, 1);

-- 3. nextval 함수 정의
DELIMITER //

CREATE FUNCTION nextval(s_name VARCHAR(50)) RETURNS BIGINT
    DETERMINISTIC
BEGIN
    DECLARE val BIGINT;

    -- 해당 시퀀스 행에 락을 걸고 값을 업데이트
    UPDATE sys_sequences
    SET current_value = current_value + increment_by
    WHERE sequence_name = s_name;

    -- 업데이트된 값을 변수에 담아 반환
    SELECT current_value INTO val
    FROM sys_sequences
    WHERE sequence_name = s_name;

    RETURN val;
END //

DELIMITER ;