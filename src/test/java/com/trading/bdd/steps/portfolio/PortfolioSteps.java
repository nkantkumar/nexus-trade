package com.trading.bdd.steps.portfolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trading.portfolio.PortfolioExecutionEvent;
import com.trading.portfolio.PortfolioService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Component
public class PortfolioSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PortfolioService portfolioService;

    private MvcResult lastResponse;

    @Given("account {word} executed a fill for {long} {word} shares at price {double}")
    public void executedFill(String accountId, long qty, String symbol, double price) {
        portfolioService.onExecution(new PortfolioExecutionEvent(accountId, symbol, qty, price));
    }

    @When("I request the portfolio snapshot for symbol {word}")
    public void requestSnapshot(String symbol) throws Exception {
        lastResponse = mockMvc.perform(get("/portfolio/{symbol}", symbol)).andExpect(status().isOk()).andReturn();
    }

    @Then("the snapshot quantity for symbol {word} is {long}")
    public void assertQuantity(String symbol, long quantity) throws Exception {
        mockMvc.perform(get("/portfolio/{symbol}", symbol))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value(symbol))
                .andExpect(jsonPath("$.quantity").value(quantity));
        assertThat(lastResponse).isNotNull();
    }
}
