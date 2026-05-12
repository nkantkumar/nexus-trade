## Marketdata:-
┌─────────────────────────────────────────────────────────────┐
│                     Your Trading System                      │
├───────────────┬─────────────────┬───────────────────────────┤
│  FIX Engine   │  OUCH Engine    │      Matching Engine      │
│ (QuickFIX/J)  │ (Binary/UDP)    │      (Core Matching)      │
├───────────────┴─────────────────┴───────────────────────────┤
│                    Protocol Adapter Layer                    │
│         (Converts FIX/OUCH ↔ Internal Order Format)         │
└─────────────────────────────────────────────────────────────┘
↕
┌─────────────────────────────────────────────────────────────┐
│                         Exchange                            │
│         FIX Session ────┐         ┌──── OUCH Session        │
│                         │         │                         │
│                    ┌────▼─────────▼────┐                    │
│                    │  Exchange Gateway │                    │
│                    └───────────────────┘                    │
└─────────────────────────────────────────────────────────────┘

## OUCH 5.0
OUCH 5.0 Message Structure (binary):
┌─────────────────────────────────────────────────────┐
│ Byte 0-1:  Message Length (uint16)                  │
│ Byte 2:     Message Type (char)                     │
│ Byte 3-6:   User Reference Number (uint32)          │
│ Byte 7-10:  Quantity (uint32)                       │
│ Byte 11-18: Price (uint64, fixed-point)             │
│ Byte 19-26: Symbol (8 bytes, fixed-length)          │
│ Byte 27:    Side (char: 'B' or 'S')                 │
│ Byte 28:    Time in Force (char)                    │
└─────────────────────────────────────────────────────┘
-------------------------------------------------------------------------------------------------------
┌─────────────────────────────────────────────────────────────────┐
│                        Risk Engine                               │
├───────────────┬───────────────┬───────────────┬─────────────────┤
│  Pre-Trade    │  Position     │  Margin       │  Credit         │
│  Validators   │  Manager      │  Calculator   │  Manager        │
├───────────────┴───────────────┴───────────────┴─────────────────┤
│                    Risk Rules Engine                              │
│         (Configurable rules with dynamic updates)                │
└─────────────────────────────────────────────────────────────────┘
↕
┌─────────────────────────────────────────────────────────────────┐
│                      Matching Engine                             │
│         (Checks risk before accepting any order)                 │
└─────────────────────────────────────────────────────────────────┘

Position Limits: Per-symbol and net position limits

Exposure Limits: Gross and net exposure tracking

Fat Finger Checks: Price deviation, order size, statistical anomalies

Margin Checks: Initial margin, maintenance margin, margin calls

Credit Checks: Credit limits, credit ratings, real-time usage

Rate Limiting: Orders per second, value per second

Duplicate Detection: Prevents duplicate submissions

Real-time Monitoring: Live risk dashboard with metrics

Automatic Liquidation: Margin call handling

Risk Event Publishing: Complete audit trail

┌─────────────────────────────────────────────────────────────┐
│                     TradingRiskApplication                    │
│                         (Main Setup)                          │
└─────────────────────┬───────────────────────────────────────┘
│
├── Creates ──► MarketDataProvider
├── Creates ──► CreditManager  
└── Creates ──► MarginManager
│
└── Uses ──► MarketDataProvider

┌─────────────────────▼───────────────────────────────────────┐
│                       RiskEngine                             │
│  Dependencies:                                               │
│  ├─ CreditManager (injected)                                │
│  ├─ MarginManager (injected)                                │
│  └─ MarketDataProvider (injected)                           │
└─────────────────────┬───────────────────────────────────────┘
│
├── Uses ──► CreditValidator
│            └─ Uses CreditManager
│
├── Uses ──► MarginValidator  
│            ├─ Uses MarginManager
│            └─ Uses MarketDataProvider
│
├── Uses ──► FatFingerValidator
│            └─ Uses MarketDataProvider
│
└── Uses ──► PostTradeRiskValidator
└─ Uses MarketDataProvider
## ------------------------------------------------------------------------------