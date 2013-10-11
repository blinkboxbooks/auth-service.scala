# Zuul Server Change Log

## 0.4.0 (2013-10-10 12:52)

### Breaking Changes

- Endpoint `/tokeninfo` has been renamed to `/session`.

### New Features

- Welcome email sent when a new user registered successfully.

### Bug Fixes

- Password reset link format now matches what the website is expecting in example properties files.

## 0.3.0 (2013-10-07 12:32)

### New Features

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

- New endpoint `POST /password/change` to allow users to change their password.

## 0.1.0 (Baseline Release)

Baseline release from which the change log was started. Database schema version should be 5 at this point.
