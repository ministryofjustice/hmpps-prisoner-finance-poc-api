# Process flows for Prisoner finance

## Purchasing

### 1. Ordering within available spends limit

This would be the most common payment process, it is only effective for a pre-delivery process where the payment is taken before the purchase is fulfilled (such as with Amazon or other ecommerce services). Failure will occur if there is not enough available spends to complete the purchase, even if the prisoner has more funds in their private cash or savings sub-accounts.

```mermaid
sequenceDiagram
  actor USER as User
  participant SHOP as Shop
  box Prisoner Finance
  participant API as Payments Service
  participant LEDGER as Ledger
  participant ADI as Reconciliation Service
  end
  box Single Operating Platform
  participant SOP as General ledger
  end

  autonumber

  USER ->> SHOP: Enter order details
  activate SHOP
  SHOP ->> SHOP: Create <PaymentRequest>
  SHOP ->> SHOP: Add supplier payment
  SHOP ->> API: Submit <PaymentRequest>
  deactivate SHOP
  activate API
  API ->> LEDGER: Check available spends
  LEDGER -->> API: Available spends
  API -->> API: Confirm funds available
  API ->> LEDGER: Create <pending><br/>prisoner debit transaction
  activate LEDGER
  LEDGER -->> API: Success response
  API -->> SHOP: Success response
  SHOP -->> USER: Check details
  USER ->> SHOP: Confirm details
  SHOP ->> API: Authorise <PaymentRequest>
  API -) ADI: Append ADI instructions
  API -->> SHOP: Authorization result
  deactivate API
  SHOP -->> USER: Checkout complete
  SHOP -->> USER: Email receipt
  ADI ->> SOP: Upload ADI
  SOP ->> SOP: Write general ledger entries
  SOP -->> ADI: Success response
  ADI ->> LEDGER: Set status of ledger entries to processed
  deactivate LEDGER
```

### 2. Recording a purchase from private cash

This process would take place in circumstances where items are not limited to the available spends limit. It is primarily for emergency purchases or items that are not restricted to available spends such as money to friends and family members. Failure will occur if there is not enough private cash to complete the purchase, even if the prisoner has more funds in their available spends or savings sub-accounts.

```mermaid
sequenceDiagram
  actor USER as User
  participant SHOP as Shop
  box Prisoner Finance
  participant API as Payments Service
  participant LEDGER as Ledger
  participant ADI as Reconciliation Service
  end
  box Single Operating Platform
  participant SOP as General ledger
  end

  autonumber

  USER ->> SHOP: Enter purchase details
  activate SHOP
  SHOP ->> SHOP: Create <PaymentRequest>
  SHOP ->> SHOP: Add supplier payment
  SHOP ->> API: Submit <PaymentRequest>
  deactivate SHOP
  activate API
  API ->> LEDGER: Check private cash
  LEDGER -->> API: Private cash balance
  API -->> API: Confirm funds available
  API ->> LEDGER: Create <pending><br/>prisoner debit transaction
  activate LEDGER
  LEDGER -->> API: Success response
  API -->> SHOP: Success response
  SHOP -->> USER: Check details
  USER ->> SHOP: Confirm details
  SHOP ->> API: Authorise <PaymentRequest>
  API -) ADI: Append ADI instructions
  API -->> SHOP: Authorization result
  deactivate API
  SHOP -->> USER: Checkout complete
  SHOP -->> USER: Email receipt
  ADI ->> SOP: Upload ADI
  SOP ->> SOP: Write general ledger entries
  SOP -->> ADI: Success response
  ADI ->> LEDGER: Set status of ledger entries to processed
  deactivate LEDGER
```

### 3. Ordering using available spends and additional private cash

This process would take place in circumstances such as dental treatment or where the governor has given permission for the prisoner to spend more than their available spends such as after a compensation payment. Failure will occur if there is not enough available spends and private cash to complete the purchase, even if the prisoner has more funds in their savings sub-accounts.

```mermaid
sequenceDiagram
  actor USER as User
  participant SHOP as Shop
  box Prisoner Finance
  participant API as Payments Service
  participant LEDGER as Ledger
  participant ADI as Reconciliation Service
  end
  box Single Operating Platform
  participant SOP as General ledger
  end

  autonumber

  USER ->> SHOP: Enter purchase amount
  activate SHOP
  SHOP ->> SHOP: Create <PaymentRequest>
  SHOP ->> SHOP: Add supplier payment
  SHOP ->> API: Submit <PaymentRequest>
  deactivate SHOP
  activate API
  API ->> LEDGER: Check available spends
  LEDGER -->> API: Available spends
  API -->> API: Confirm funds not available
  API ->> LEDGER: Check private cash
  LEDGER -->> API: Private cash balance
  API -->> API: Confirm additional funds available
  API ->> LEDGER: Create two <pending><br/>prisoner debit ledger entries
  activate LEDGER
  note right of LEDGER: Seperate ledger entries<br/>from each account to<br/>the supplier with the<br/>same description and<br/>transaction id.
  LEDGER -->> API: Success response
  API -->> SHOP: Success response
  SHOP -->> USER: Check details
  USER ->> SHOP: Confirm details
  SHOP ->> API: Authorise <PaymentRequest>
  API -) ADI: Append ADI instructions
  API -->> SHOP: Authorization result
  deactivate API
  SHOP -->> USER: Checkout complete
  SHOP -->> USER: Email receipt
  ADI ->> SOP: Upload ADI
  SOP ->> SOP: Write general ledger entries
  SOP -->> ADI: Success response
  ADI ->> LEDGER: Set status of ledger entries to processed
  deactivate LEDGER
```

### 4. Purchasing with advance only

This process would take place in circumstances where the prisoner does not have any money available to spend such as when they first arrive in prison.

It is normal for governor approval to be sought but there can also be a prison wide policy around automatic supply of grants in specific circumstances such as transfers from a private prison or arrival without cash. Failure will occur if the total amount is more than allowed by an advance.

```mermaid
sequenceDiagram
  actor USER as User
  participant SHOP as Shop
  box Prisoner Finance
  participant API as Payments Service
  participant LEDGER as Ledger
  participant REPAY as Repayments servcie
  participant ADI as Reconciliation Service
  end
  box Single Operating Platform
  participant SOP as General ledger
  end

  autonumber

  USER ->> SHOP: Enter order details
  activate SHOP
  SHOP ->> SHOP: Create <PaymentRequest>
  SHOP ->> SHOP: Add supplier payment
  SHOP ->> API: Submit <PaymentRequest>
  deactivate SHOP
  activate API
  API ->> LEDGER: Check available spends
  LEDGER -->> API: Available spends
  API -->> API: Confirm funds not available
  API ->> LEDGER: Check private cash
  LEDGER -->> API: Private cash balance
  API -->> API: Confirm additional funds not available
  API -->> SHOP: Failure response
  deactivate API
  SHOP -->> USER: Recommend advance
  USER ->> SHOP: Request advance
  SHOP ->> API: Approve advance
  activate API
  API ->> API: Create <AdvanceRequest>
  API ->> API: Add prisoner advance
  API ->> REPAY: Submit <AdvanceRequest>
  activate REPAY
  deactivate API
  REPAY ->> LEDGER: Create <pending><br/>advance setup transaction
  REPAY ->> LEDGER: Create <pending><br/>prisoner credit transaction
  activate LEDGER
  REPAY -->> API: Success response
  deactivate REPAY
  API ->> LEDGER: Create <pending><br/>prisoner debit transaction
  LEDGER -->> API: Success response
  API -->> SHOP: Success response
  SHOP -->> USER: Check details
  USER ->> SHOP: Confirm details
  SHOP ->> API: Authorise <PaymentRequest>
  API ->> REPAY: Authorise <AdvanceRequest>
  activate REPAY
  REPAY -) ADI: Append ADI instructions
  REPAY -> REPAY: Setup payment schedule
  REPAY -->> API: Success response
  deactivate REPAY
  API -) ADI: Append ADI instructions
  API -->> SHOP: Authorization result
  SHOP -->> USER: Checkout complete
  SHOP -->> USER: Email receipt
  ADI ->> SOP: Upload ADI
  SOP ->> SOP: Write general ledger entries
  SOP -->> ADI: Success response
  ADI ->> LEDGER: Set status of ledger entries to processed
  deactivate LEDGER

```

### 5. Ordering using available funds and advance

```mermaid
sequenceDiagram
  actor User as User
```

### 6. Purchasing from multiple suppliers

This process is often employed as part of the canteen or tuck shop processes where several different items can be purchased from several different suppliers such as BT and DHL.

The process will consider the complete payment total when determining whether there are enough available funds, but will record individual ledger entries for each supplier with the amount spent with each.

```mermaid
sequenceDiagram
  actor USER as User
  participant SHOP as Shop
  box Prisoner Finance
  participant API as Payments Service
  participant LEDGER as Ledger
  participant ADI as Reconciliation Service
  end
  box Single Operating Platform
  participant SOP as General ledger
  end

  autonumber

  USER ->> SHOP: Enter order details
  activate SHOP
  SHOP ->> SHOP: Create <PaymentRequest>
  SHOP ->> SHOP: Add supplier payment
  SHOP ->> SHOP: Add second supplier payment
  SHOP ->> API: Submit <PaymentRequest>
  deactivate SHOP
  activate API
  API ->> LEDGER: Check available spends
  LEDGER -->> API: Available spends
  API -->> API: Confirm funds available
  API ->> LEDGER: Create two <pending><br/>prisoner debit ledger entries
  activate LEDGER
  LEDGER -->> API: Success response
  API -->> SHOP: Success response
  SHOP -->> USER: Check details
  USER ->> SHOP: Confirm details
  SHOP ->> API: Authorise <PaymentRequest>
  API -) ADI: Append ADI instructions
  API -->> SHOP: Authorization result
  deactivate API
  SHOP -->> USER: Checkout complete
  SHOP -->> USER: Email receipt
  ADI ->> SOP: Upload ADI
  SOP ->> SOP: Write general ledger entries
  SOP -->> ADI: Success response
  ADI ->> LEDGER: Set status of ledger entries to processed
  deactivate LEDGER
```

### 7. Purchasing for several people from same supplier

This process is often employed for batch processing where several items have been ordered by several different people.

The process will consider the payment total for each person when determining whether there are enough available funds, and will record individual ledger entries for each person with the amount spent by each.

```mermaid
sequenceDiagram
  actor USER as User
  participant SHOP as Shop
  box Prisoner Finance
  participant API as Payments Service
  participant LEDGER as Ledger
  participant ADI as Reconciliation Service
  end
  box Single Operating Platform
  participant SOP as General ledger
  end

  autonumber

  USER ->> SHOP: Enter order details
  activate SHOP
  SHOP ->> SHOP: Create <PaymentRequest>
  SHOP ->> SHOP: Add supplier payment
  SHOP ->> SHOP: Create second <PaymentRequest>
  SHOP ->> SHOP: Add supplier payment
  SHOP ->> API: Submit <PaymentRequest> batch
  deactivate SHOP
  activate API
  API ->> LEDGER: Check available spends
  LEDGER -->> API: Available spends
  API -->> API: Confirm funds available
  API ->> LEDGER: Check second available spends
  LEDGER -->> API: Available spends
  API -->> API: Confirm funds available
  API ->> LEDGER: Create two <pending><br/>prisoner debit ledger entries
  activate LEDGER
  LEDGER -->> API: Success response
  API -->> SHOP: Success response
  SHOP -->> USER: Check details
  USER ->> SHOP: Confirm details
  SHOP ->> API: Authorise <PaymentRequest> batch
  API -) ADI: Append ADI instructions
  API -->> SHOP: Authorization result
  deactivate API
  SHOP -->> USER: Checkout complete
  SHOP -->> USER: Email receipt
  ADI ->> SOP: Upload ADI
  SOP ->> SOP: Write general ledger entries
  SOP -->> ADI: Success response
  ADI ->> LEDGER: Set status of ledger entries to processed
  deactivate LEDGER
```

### 8. Purchasing for several people from multiple suppliers

This process is employed when batch processing multiple order as part of the canteen process or similar where several different items can be purchased from several different suppliers such as BT and DHL for several different people.

The process will consider the complete payment total per person when determining whether there are enough available funds, and will record individual ledger entries for each supplier for each person with the amount spent with each.

```mermaid
sequenceDiagram
  actor USER as User
  participant SHOP as Shop
  box Prisoner Finance
  participant API as Payments Service
  participant LEDGER as Ledger
  participant ADI as Reconciliation Service
  end
  box Single Operating Platform
  participant SOP as General ledger
  end

  autonumber

  USER ->> SHOP: Enter order details
  activate SHOP
  SHOP ->> SHOP: Create <PaymentRequest>
  SHOP ->> SHOP: Add supplier payment
  SHOP ->> SHOP: Add second supplier payment
  SHOP ->> SHOP: Create second <PaymentRequest>
  SHOP ->> SHOP: Add supplier payment
  SHOP ->> SHOP: Add third supplier payment
  SHOP ->> API: Submit <PaymentRequest> batch
  deactivate SHOP
  activate API
  API ->> LEDGER: Check available spends
  LEDGER -->> API: Available spends
  API -->> API: Confirm funds available
  API ->> LEDGER: Check second available spends
  LEDGER -->> API: Available spends
  API -->> API: Confirm funds available
  API ->> LEDGER: Create four <pending><br/>prisoner debit ledger entries
  activate LEDGER
  LEDGER -->> API: Success response
  API -->> SHOP: Success response
  SHOP -->> USER: Check details
  USER ->> SHOP: Confirm details
  SHOP ->> API: Authorise <PaymentRequest> batch
  API -) ADI: Append ADI instructions
  API -->> SHOP: Authorization result
  deactivate API
  SHOP -->> USER: Checkout complete
  SHOP -->> USER: Email receipt
  ADI ->> SOP: Upload ADI
  SOP ->> SOP: Write general ledger entries
  SOP -->> ADI: Success response
  ADI ->> LEDGER: Set status of ledger entries to processed
  deactivate LEDGER
```
