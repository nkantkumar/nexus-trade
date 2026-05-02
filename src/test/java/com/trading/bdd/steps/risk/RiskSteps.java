package com.trading.bdd.steps.risk;

import static org.assertj.core.api.Assertions.assertThat;

import com.trading.risk.RiskEngineService;
import com.trading.risk.RiskResult;
import com.trading.risk.TradeIntent;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


public class RiskSteps {

    private final RiskEngineService riskEngineService;

    private RiskResult lastResult;

    @Autowired
    public RiskSteps(RiskEngineService riskEngineService) {
        this.riskEngineService = riskEngineService;
    }

    @When(
            "I run a pre-trade risk check for account {word} symbol {word} quantity {long} price {double} vwap {double} margin {double}")
    public void runCheck(String account, String symbol, long qty, double price, double vwap, double margin) {
        lastResult = riskEngineService.preTradeCheck(new TradeIntent(
                account,
                symbol,
                qty,
                BigDecimal.valueOf(price),
                BigDecimal.valueOf(vwap),
                BigDecimal.valueOf(margin)));
    }

    @Then("the risk outcome is approved")
    public void approved() {
        assertThat(lastResult).isInstanceOf(RiskResult.Approved.class);
    }

    @Then("the risk outcome is rejected")
    public void rejected() {
        assertThat(lastResult).isInstanceOf(RiskResult.Rejected.class);
    }
}
