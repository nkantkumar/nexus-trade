package com.trading.bdd.steps.hooks;

import com.trading.identity.IdentityService;
import com.trading.order.OrderRepository;
import com.trading.portfolio.PortfolioService;
import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


public class BddHooks {

    private final OrderRepository orderRepository;
    private final PortfolioService portfolioService;
    private final IdentityService identityService;

    @Autowired
    public BddHooks(OrderRepository orderRepository, PortfolioService portfolioService, IdentityService identityService) {
        this.orderRepository = orderRepository;
        this.portfolioService = portfolioService;
        this.identityService = identityService;
    }

    @Before
    public void resetMutableState() {
        orderRepository.deleteAll();
        portfolioService.resetForTests();
        identityService.resetSecurityStateForTests();
    }
}
