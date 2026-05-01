@team-identity @domain-identity @contract:auth-behavior
Feature: Identity authentication outcomes

  Scenario: Invalid password is rejected before MFA succeeds
    When I attempt login as trader1 with password wrongpass and totp 000000
    Then authentication is rejected
