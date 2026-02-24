### 4.6 POST /api/bids/create   // not work
Create a new bid.

**Request Body:**
```json
{
  "tenderId": 46,
  "bidderId": 1,
  "bidAmount": 5000000,
  "proposalText": "Our company can complete this project...",
  "contactNumber": "9876543210",
  "status": "PENDING"
}
```

**Test Command:**
```bash
curl -s -X POST http://localhost:8080/api/bids/create \
  -H "Content-Type: application/json" \
  -d '{"tenderId":46,"bidderId":1,"bidAmount":5000000,"proposalText":"Test proposal","status":"PENDING"}'
```

---