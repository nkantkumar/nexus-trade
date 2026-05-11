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