# API Reference

## Authentication

### Register user
`POST /auth/register`

Request body:
```json
{
  "email": "user@example.com",
  "password": "StrongPass123!"
}
```

Response:
```json
{
  "userId": 1
}
```

### Verify user
`POST /auth/verify`

Request body:
```json
{
  "token": "verification-token"
}
```

### Login
`POST /auth/login`

Request body:
```json
{
  "email": "user@example.com",
  "password": "StrongPass123!"
}
```

Response:
```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "opaque-refresh-token"
}
```

### Refresh token
`POST /auth/refresh`

Request body:
```json
{
  "refreshToken": "opaque-refresh-token"
}
```

### List sessions
`GET /auth/sessions`

Headers:
```http
Authorization: Bearer <access-token>
```

## Public profile
`GET /p/{username}`

## Update current profile
`PUT /me/profile`

Headers:
```http
Authorization: Bearer <access-token>
```

Request body:
```json
{
  "username": "jdoe",
  "displayName": "John Doe"
}
```

## Organization operations
### Create organization
`POST /orgs`

### Get organization by slug
`GET /orgs/{slug}`

### Add member
`POST /orgs/{orgId}/members`

### List members
`GET /orgs/{orgId}/members`

### Create team
`POST /orgs/{orgId}/teams`

## NFC lifecycle
### Tap card
`GET /tap/{internalCode}`

### Assign card to profile
`POST /nfc/{id}/assign`

Request body:
```json
{
  "internalCode": "ABC123",
  "profileId": 5
}
```

### Report lost card
`POST /nfc/{id}/report-lost`

### Retire card (admin)
`POST /admin/nfc/{id}/retire`

## Orders
### Create order
`POST /orders`

### Get order by id
`GET /orders/{id}`

## Subscriptions
### Create subscription
`POST /subscriptions`

## Admin
### Inventory
`GET /admin/nfc/inventory`

### Orders listing
`GET /admin/orders`
