package com.trading.settlement.dvp;

/**
 * Delivery versus Payment (DvP) Settlement Models defined by BIS (Bank for International Settlements).
 */
public enum DvPModel {
    MODEL_1, // Gross Cash & Gross Securities settlement on a continuous trade-by-trade basis
    MODEL_2, // Net Cash & Gross Securities settlement
    MODEL_3  // Net Cash & Net Securities settlement at end of clearing window
}
