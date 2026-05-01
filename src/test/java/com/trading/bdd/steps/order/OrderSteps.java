package com.trading.bdd.steps.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.trading.order.CreateOrderCommand;
import com.trading.order.OrderRepository;
import com.trading.order.OrderService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderSteps {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    private UUID lastOrderId;

    @Autowired
    public OrderSteps(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @Given("the order book has no persisted orders")
    public void noPersistedOrders() {
        orderRepository.deleteAll();
    }

    @When("I create an order for symbol {word} quantity {long} limit price {string}")
    public void createOrder(String symbol, long quantity, String limitPrice) {
        lastOrderId = orderService.create(new CreateOrderCommand(symbol, quantity, new BigDecimal(limitPrice)));
    }

    @Then("a persisted order exists for symbol {word}")
    public void assertSymbolExists(String symbol) {
        assertThat(orderRepository.findAll()).anyMatch(o -> symbol.equals(o.getSymbol()));
        assertThat(lastOrderId).isNotNull();
        assertThat(orderRepository.findById(lastOrderId)).isPresent();
    }
}
