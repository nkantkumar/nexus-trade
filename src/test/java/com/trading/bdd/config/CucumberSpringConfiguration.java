package com.trading.bdd.config;

import com.trading.NexusTradeApplication;
import com.trading.bdd.support.BddRedisMockConfiguration;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(classes = NexusTradeApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles({"bdd", "test"})
@EmbeddedKafka(partitions = 1, topics = {"order-events", "execution-events", "notifications"})
@Import(BddRedisMockConfiguration.class)
public class CucumberSpringConfiguration {}
