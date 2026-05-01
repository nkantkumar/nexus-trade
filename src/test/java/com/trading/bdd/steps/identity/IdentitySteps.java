package com.trading.bdd.steps.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.trading.identity.IdentityService;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IdentitySteps {

    private final IdentityService identityService;

    private IdentityService.AuthResult lastAuth;

    @Autowired
    public IdentitySteps(IdentityService identityService) {
        this.identityService = identityService;
    }

    @When("I attempt login as {word} with password {word} and totp {word}")
    public void login(String user, String password, String totp) {
        lastAuth = identityService.authenticate(new IdentityService.LoginRequest(user, password, totp));
    }

    @Then("authentication is rejected")
    public void rejected() {
        assertThat(lastAuth).isInstanceOf(IdentityService.AuthResult.Rejected.class);
    }
}
