# Zuul Server Change Log

## 0.16.4 ([#34](https://git.mobcastdev.com/Zuul/auth-service/pull/34) 2014-08-29 10:34:05)

Fix rpm submodule

### Improvements

- RPM submodule now references a commit that exists.

## 0.16.3 ([#28](https://git.mobcastdev.com/Zuul/auth-service/pull/28) 2014-08-20 15:44:08)

Rename SsoAccessToken -> SsoDecodedAccessToken

Improvement

This is necessary to avoid potential name clashes in case-insensitive filesystems.

## 0.16.2 ([#27](https://git.mobcastdev.com/Zuul/auth-service/pull/27) 2014-08-20 15:16:13)

Fix submodule commit

Patch to fix the submodule commit (the last PR broke it)

## 0.16.1 ([#25](https://git.mobcastdev.com/Zuul/auth-service/pull/25) 2014-08-19 17:28:28)

Use the fat jar RPM specs rather than the ruby ones

Patch to make the RPM build properly.

## 0.16.0 ([#24](https://git.mobcastdev.com/Zuul/auth-service/pull/24) 2014-08-19 11:04:50)

CP-1668 Implement session extension endpoint

New feature

One of the last endpoints that have integration with SSO. Almost there...

## 0.15.2 ([#23](https://git.mobcastdev.com/Zuul/auth-service/pull/23) 2014-08-19 09:52:47)

CP-1667 Introduce json serializer for Elevation

Bugfix

The reason for failing tests is that the session wasn't being correctly de-serialized because the `Elevation` serializer was missing.

## 0.15.1 ([#22](https://git.mobcastdev.com/Zuul/auth-service/pull/22) 2014-08-18 17:44:16)

Fixed expiry date of token

### Bug fixes

- The `exp` claim in a JWT should be an `IntDate` not an offset.
- The `zl/rti` claim is an `Int` not a `String`.

## 0.15.0 ([#21](https://git.mobcastdev.com/Zuul/auth-service/pull/21) 2014-08-18 14:55:00)

CP-1667 Implement query-session endpoint

New feature

This PR implements the endpoint that queries the current session status. Please note that on SSO there are only two possible elevations (critical and none) while we have three. 

It's also worth having a look at how we deal with the cases where we don't have an SSO token for the provided Zuul token. 

Finally there is something strange going on with spray-tests for endpoints that need authentication: I marked them as cancelled at the moment, if you have any idea about why the endpoint answers with a 401 when I try to run them it will be very welcome.

## 0.14.1 ([#20](https://git.mobcastdev.com/Zuul/auth-service/pull/20) 2014-08-18 12:37:56)

Changed project name to match convention

Tiny patch to change the name of this to match our usual conventions.

## 0.14.0 ([#19](https://git.mobcastdev.com/Zuul/auth-service/pull/19) 2014-08-15 14:02:02)

Integrate refresh token revocation endpoint with SSO

New feature

The title explains it all.

## 0.13.2 ([#18](https://git.mobcastdev.com/Zuul/auth-service/pull/18) 2014-08-14 15:28:19)

Make test not depending on the sso key path

Improvements

This should make tests runnable on TeamCity.

## 0.13.1 ([#105](https://git.mobcastdev.com/Zuul/zuul-server/pull/105) 2014-07-21 08:25:22)

Use percent

### Bug fix

- Use percent instead of dollar in spec files!

## 0.13.0 ([#103](https://git.mobcastdev.com/Zuul/zuul-server/pull/103) 2014-07-18 15:06:38)

Prepare for RPM building

### New Feature

- Allows building RPMs!

![Everything is awesome](http://media.giphy.com/media/Z6f7vzq3iP6Mw/giphy.gif)

## 0.12.1 ([#99](https://git.mobcastdev.com/Zuul/zuul-server/pull/99) 2014-07-03 10:44:16)

Updated README

### Improvement

- updated README

## 0.12.0 ([#98](https://git.mobcastdev.com/Zuul/zuul-server/pull/98) 2014-06-30 16:18:52)

New Relic integration

### New Feature

- Added first-stage New Relic integration.
- Upgraded to using Artifactory for gems.

## 0.11.7 (2014-02-28 18:52)

## Bug Fixes

- [CP-1217](https://tools.mobcastdev.com/jira/browse/CP-1217): Unsigned tokens cannot be used to authenticate.

## 0.11.6 (2014-02-14 17:17)

## Bug Fixes

- [CP-1096](https://tools.mobcastdev.com/jira/browse/CP-1096): Fixed an issue where a user registration with a client will resulting in publishing a reporting message without a user_id for the respective client.

## 0.11.5 (2014-02-13 15:47)

### Bug Fixes

- Fix an issue with the 0.11.3 deployment script which removed some roles from the super user instead of adding to them

## ~~0.11.4 (2014-02-13 15:24)~~

_There is an issue with a DB deployment script in this release. Don't use it._

### Bug Fixes

- [CP-1141](https://tools.mobcastdev.com/jira/browse/CP-1141): Fixed incorrect content-type return value on some routes (inc `/session`)

## ~~0.11.3 (2014-02-13 10:40)~~

_There is an issue with returned media types and a DB deployment script in this release. Don't use it._

### Improvements

- Added 'mer' (Merchandising) and 'mkt' (Marketing) roles.
- Added non-null constraints to roles/privileges tables.

### Deployment Notes

- A database migration to schema version 12 is required.

## ~~0.11.2 (2014-02-11 09:22)~~

_There is an issue with returned media types in this release. Don't use it._

### Improvements

- Removed dependency on MultiJson in favour of built-in JSON library.

## 0.11.1 (2014-02-10 18:10)

### Bug Fixes

- [CP-1108](https://tools.mobcastdev.com/jira/browse/CP-1108) - Registering users is now insensitive to the case of the email address.

## 0.11.0 (2014-01-23 18:12)

### New Features

- [CP-1044](https://tools.mobcastdev.com/jira/browse/CP-1044) - Authenticated events are now sent when a user authenticates to the server.

## 0.10.3 (2014-01-29 15:20)

### Bug Fixes

- Cucumber and RSpec dependencies are now lazily loaded by the Rakefile so that it can deploy successfully without development/test dependencies.

## 0.10.2 (2014-01-23 14:28)

### New Features

- [CP-911](https://tools.mobcastdev.com/jira/browse/CP-911) - Administrative retrieval of user information by identifier is now supported.

## 0.10.1 (2014-01-22 17:55)

### Bug Fixes

- [CP-988](https://tools.mobcastdev.com/jira/browse/CP-988) - Timing is now correct for throttled login attempts (was too long by up to 1 second).

## 0.10.0 (2014-01-21 09:58)

### New Features

- [CP-910](https://tools.mobcastdev.com/jira/browse/CP-910) - Administrative search for users is now supported by username, first and last name, or user id.
- Username change history is now recorded, to support user search by previous username (i.e. the search returns any user who has ever had that username).

### Deployment Notes

- A database migration to schema version 10 is required.

## 0.9.0 (2014-01-16 14:57)

### New Features

- [CP-557](https://tools.mobcastdev.com/jira/browse/CP-557) - Users can now be associated with roles, which are returned in the token and in session info.

### Deployment Notes

- A database migration to schema version 9 is required.

## 0.8.2 (2014-01-14 12:28)

### Improvements

- [CP-990](https://tools.mobcastdev.com/jira/browse/CP-990):
    - Using simpler XML schema for reporting
    - Switched from fanout to topic exchange with routing key

## 0.8.1 (2014-01-13 18:25)

### New Features

- [CP-968](https://tools.mobcastdev.com/jira/browse/CP-968) - Auth server now logs errors and warnings to file in JSON format.

### Deployment Notes

- Two new properties are required in the properties file:
    - `logging.error.file` - The error log file.
    - `logging.error.level` - The error log level.

## 0.8.0 (2014-01-07 10:29)

### New Features

- [CP-313](https://tools.mobcastdev.com/jira/browse/CP-313) - Failed attempts to authenticate or change password are now throttled to 5 consecutive failures within a 20 second period.

### Bug Fixes

- Fixed an (unreported) timing bug in user authentication where no password hashing was done for unregistered email addresses, meaning that it would be possible to check whether an address was registered by inspecting the response time.

### Deployment Notes

- A database migration to schema version 8 is required.

## 0.7.2 (2013-12-23 11:00)

### New Features

- [CP-920](https://tools.mobcastdev.com/jira/browse/CP-920) - Reporting support for user and client events.

## 0.7.1 (2013-12-18 14:06)

### New Features

- [CP-907](https://tools.mobcastdev.com/jira/browse/CP-907) - Performance logger now logs array of client IPs including `X-Forwarded-For` header information.

## 0.7.0 (2013-12-11 17:15)

### New Features

- [CP-872](https://tools.mobcastdev.com/jira/browse/CP-872) - Performance information for requests is now logged.

### Deployment Notes

- New properties are required in the properties file:
    - `logging.perf.file` - The performance log file.
    - `logging.perf.level` - The performance log level.
    - `logging.perf.threshold.error` - The threshold (in ms) for performance error logs.
    - `logging.perf.threshold.warn` - The threshold (in ms) for performance warning logs.
    - `logging.perf.threshold.info` - The threshold (in ms) for performance info logs.

## 0.6.2 (2013-11-06 15:51)

### Bug Fixes

- [CP-765](https://tools.mobcastdev.com/jira/browse/CP-765) - Fixed a false positive test so now the server should return a client secret when using combined user and client registration.

## 0.6.1 (2013-11-05 19:28)

### Bug Fixes

- [CP-581](https://tools.mobcastdev.com/jira/browse/CP-581) - Fixed a bug where we wouldn't extend the elevation period right after an action that required elevation.
    - Refactored the elevation checks along with the extension in a sinatra filter (before and after).
    - Added constants for elevation expiry timespans.

## 0.6.0 (2013-11-05 09:54)

### New Features

- [CP-714](https://tools.mobcastdev.com/jira/browse/CP-714) - Adding simultaneous user and client registrations. The implication of which are as follows:
    - From now on, client registration will require all fields, which are os, model, name and brand.
    - The old user registration works as before, however, if client info is added to the request, it will trigger both a user registration and client registration.
    - User registration is now done as an SQL transaction, meaning that if user registration or client registration fails, neither will be created in our database.

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
