package com.trading.bdd.steps.referencedata;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;


public class ReferenceDataSteps {

    @Autowired
    private MockMvc mockMvc;

    @When("I request reference metadata for symbol {word}")
    public void requestSymbol(String symbol) throws Exception {
        mockMvc.perform(get("/reference-data/symbols/{symbol}", symbol)).andExpect(status().isOk());
    }

    @Then("symbol {word} resolves to exchange {word}")
    public void assertExchange(String symbol, String exchange) throws Exception {
        mockMvc.perform(get("/reference-data/symbols/{symbol}", symbol))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value(symbol))
                .andExpect(jsonPath("$.exchange").value(exchange));
    }
}
