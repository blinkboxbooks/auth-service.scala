# Zuul Server Change Log

## 0.5.3 (2013-11-01 14:54)

### Bug Fixes

- [CP-581](https://tools.mobcastdev.com/jira/browse/CP-581) - Personal information can only be retrieved when user is critically elevated
- [CP-720](https://tools.mobcastdev.com/jira/browse/CP-720) - Corrected auth server WWW-Authenticate headers
- Fixed a bug where PATCH request on /users/{user_id} wouldn't check for critical elevation level
- Fixed a bug where POST request on /clients wouldn't check for critical elevation level
- Fixed a bug where PATCH or DELETE requests on /clients/{client_id} wouldn't check for critical elevation level

## 0.5.2 (2013-10-23 13:52)

### Bug Fixes

- [CP-722](https://tools.mobcastdev.com/jira/browse/CP-722) - Do not allow deregisterd clients to log in with their old credentials

## 0.5.1 (2013-10-23 13:52)

### Bug Fixes

- [CP-692](https://tools.mobcastdev.com/jira/browse/CP-692) - We can now deregister from maximum amount of clients, i.e. we add a new client after a deregistration of an old client.

## 0.5.0 (2013-10-15 13:45)

### Breaking Changes

- `PATCH /clients/{id}` and `PATCH /users/{id}` now return `400 Bad Request` instead of `200 OK` if no valid updateable attributes are specified.

### New Features

- [CP-490](https://tools.mobcastdev.com/jira/browse/CP-490) - A password changed confirmation email is sent on successful password change.
- [CP-632](https://tools.mobcastdev.com/jira/browse/CP-632) - Clients now have `client_brand` and `client_os` details, which are optional on registration and updates.

### Deployment Notes

- A database migration to schema version 7 is required.

## 0.4.1 (2013-10-11 16:01)

### Bug Fixes

- [CP-607](https://tools.mobcastdev.com/jira/browse/CP-607) - Empty bearer tokens now have an `invalid_token` error code.

## 0.4.0 (2013-10-10 12:52)

### Breaking Changes

- Endpoint `/tokeninfo` has been renamed to `/session`.

### New Features

- [CP-482](https://tools.mobcastdev.com/jira/browse/CP-482) - A welcome email is sent when a new user registers successfully.

### Bug Fixes

- Password reset link format now matches what the website is expecting in example properties files.

## 0.3.0 (2013-10-07 12:32)

### New Features

- [CP-314](https://tools.mobcastdev.com/jira/browse/CP-314) - Password reset functionality is now available. The end-to-end flow relies on the mailer service.
    - New endpoint `POST /password/reset` to allow a user to request a password reset email.
    - New endpoint `POST /password/reset/validate-token` to allow validation of a password reset token prior to trying to reset using it.
    - Endpoint `POST /oauth2/token` supports new grant type `urn:blinkbox:oauth:grant-type:password-reset-token` to allow a user to reset their password and authenticate using a reset token.

### Bug Fixes

- Fixed an issue where error descriptions were being returned in the `error_reason` field instead of `error_description`.
- Fixed an issue where the time a client was last used was not being updated in the password authentication flow.

### Deployment Notes

- A database migration to schema version 6 is required.
- New properties are required in the properties file:
    - `password_reset_url` - The password reset URL template.
    - `amqp_server_url` - The connection string to the AMQP server.

## 0.2.0 (2013-10-01 13:57)

### New Features

- [CP-552](https://tools.mobcastdev.com/jira/browse/CP-552) New endpoint `POST /password/change` to allow users to change their password.

## 0.1.0 (Baseline Release)

Baseline release from which the change log was started. Database schema version should be 5 at this point.
