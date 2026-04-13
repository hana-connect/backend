package com.hanaro.hanaconnect.repository;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // H2DB를 안 쓸 때 이 어노테이션 있어야 에러 x
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Rollback(false)
public class BaseRepositoryTest {
}
