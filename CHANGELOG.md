# Zuul Server Change Log

## 0.23.5 ([#83](https://git.mobcastdev.com/Zuul/auth-service/pull/83) 2014-10-29 14:46:52)

Updated dependencies and error messages

### Improvements

- Library and language dependencies updated
- Error messages now match the ones on live

## 0.23.4 ([#80](https://git.mobcastdev.com/Zuul/auth-service/pull/80) 2014-10-03 14:25:32)

Adding SSO password change and more authenticate tests

#### Test improvements
* Add tests to ensure that when a password is changed in books, it is reflected in SSO and vice versa
* Add test to ensure that someone cannot register on the books website with an email that has been registered in SSO already
* Add tests to ensure that credentials from an account registered in books can be used to log into SSO (via movies/music/books)
* Modified the wording of the features for clarity
* CP-1883 Remove test step that checks WWW-Auth header to match spec see *Scenario: No attempt to provide bearer token* on http://jira.blinkbox.local/confluence/x/yYTi



## 0.23.3 ([#78](https://git.mobcastdev.com/Zuul/auth-service/pull/78) 2014-10-02 12:33:16)

CP-1874 Ignore conflict on SSO link for existing users

### Improvement

Avoid a 500 response when an existing user is already linked on SSO but not on our database by ignoring (but logging) the conflict on SSO and completing the authentication request.

## 0.23.2 ([#79](https://git.mobcastdev.com/Zuul/auth-service/pull/79) 2014-10-02 14:55:35)

CP-1883 Fix validation error response to comply with zuul

### Bugfix

Use different errors responses in the password-reset-token validation endpoint to comply with the status quo.

## 0.23.1 ([#82](https://git.mobcastdev.com/Zuul/auth-service/pull/82) 2014-10-06 16:29:03)

CP-1871: extend the elevation token when elevation critical

bugfix


## 0.23.0 ([#65](https://git.mobcastdev.com/Zuul/auth-service/pull/65) 2014-09-26 16:38:00)

CP-1864 Implement "You have been migrated" event

### New feature

Introduce new messages that handles different responses by the SSO service when the user is being migrated from Silo to SSO. This messages should be used to send the migrated user an email about the new features introduced with SSO. 

## 0.22.13 ([#74](https://git.mobcastdev.com/Zuul/auth-service/pull/74) 2014-09-30 16:56:35)

CP-1764 Fix validate-token failure for invalid tokens

### Bugfix

This patch fixes a failure where the SSO service does not return some fields on the token status if the token is not valid.

## 0.22.12 ([#77](https://git.mobcastdev.com/Zuul/auth-service/pull/77) 2014-10-01 11:06:05)

Fix the documentation for creating mysql user.

Improvement

## 0.22.11 ([#72](https://git.mobcastdev.com/Zuul/auth-service/pull/72) 2014-09-30 14:27:22)

Update tests to remove ELEVATED token tests

### Test improvements
- Removed scenarios that require an elevated token, since tokens now go from critally-elevated to non-elevated, there is no longer an elevated state.
- Modified scenario to check that after 29 minutes of non-elevated activity, elevation drops to none (previously it was 24 hours)
- Any previously @extremely_slow test that now finishes within half an hour is now tagged as @slow
- Fixed some error messages that have slightly changed in the new scala auth-service

## 0.22.10 ([#71](https://git.mobcastdev.com/Zuul/auth-service/pull/71) 2014-09-30 10:36:55)

CP-1910 Catch and log exceptions during app initialization

### Improvements

This patch logs unrecoverable errors that happen during service initialisation to Graylog and stops the service with an error status code.

## 0.22.9 ([#70](https://git.mobcastdev.com/Zuul/auth-service/pull/70) 2014-09-29 17:07:04)

Small fixes

### Improvements

* Introduce the setting for the DNS cache TTL on the JVM in the application.conf
* Reduce some cake boilerplate by unifying some services in one module

## 0.22.8 ([#69](https://git.mobcastdev.com/Zuul/auth-service/pull/69) 2014-09-29 15:10:08)

Add note for OPS in the README.md

### Improvements

This patch adds some instructions for OPS at the top of the README.md

## 0.22.7 ([#68](https://git.mobcastdev.com/Zuul/auth-service/pull/68) 2014-09-29 11:00:46)

CP-1887 Fix error reasons for invalid password updates

### Bugfix

This patch fixes some wrong errors returned by the password update endpoint.

## 0.22.6 ([#67](https://git.mobcastdev.com/Zuul/auth-service/pull/67) 2014-09-29 10:29:07)

CP-1886 Fix admin endpoint returning empty 200 for not found

### Bugfix

This patch fixes a wrong answer given for accessing details of a non-existing user trough the admin endpoint.

## 0.22.5 ([#66](https://git.mobcastdev.com/Zuul/auth-service/pull/66) 2014-09-29 10:08:39)

CP-1885 Fix parameters for admin endpoints

### Patch

Fixes a bug not handling query parameters on the admin endpoints.

## 0.22.4 ([#64](https://git.mobcastdev.com/Zuul/auth-service/pull/64) 2014-09-26 13:34:33)

Fixed Akka logging

### Bugfix

- Akka log messages now go to Graylog ([CP-1879](http://jira.blinkbox.local/jira/browse/CP-1879))

## 0.22.3 ([#63](https://git.mobcastdev.com/Zuul/auth-service/pull/63) 2014-09-26 10:30:29)

Fix bugs preventing deploy

### Bugfix

* Fix configuration failure
* Fix classpath resource resolution for GeoIP database in fatjar

## 0.22.2 ([#62](https://git.mobcastdev.com/Zuul/auth-service/pull/62) 2014-09-25 13:02:42)

Deploy

### Patch

This PR fixes an issue with the DNS cache of the JVM and uses a unique configuration key for setting the path of the keys to use.

## 0.22.1 ([#61](https://git.mobcastdev.com/Zuul/auth-service/pull/61) 2014-09-24 11:59:42)

Acceptance tests - sso updates

### Test improvement
- Remove PENDING from steps that are now implemented
- Add tests to create SSO users linked to movies/music and attempt to log in via books

## 0.22.0 ([#60](https://git.mobcastdev.com/Zuul/auth-service/pull/60) 2014-09-22 17:18:12)

Implement administrative APIs

### New features

* Tracking of username change
* User search admin endpoint
* User details admin endpoint
* Reduce logging on the connection pool to have clearer logs 

## 0.21.5 ([#59](https://git.mobcastdev.com/Zuul/auth-service/pull/59) 2014-09-19 09:16:28)

Fix interpretation of SSO authentication error

### Bugfix

This patch fixes the interpretation of authentication errors from SSO as, when provided wrong credentials, it responds with a 400 and a specific error field.

## 0.21.4 ([#58](https://git.mobcastdev.com/Zuul/auth-service/pull/58) 2014-09-17 12:41:14)

CP-1833 Fix time-out happening after the server was idle for a while

### Bugfix

This patch will fix an issue where, if left idle for a while, the auth-server would not be able to re-connect to the SSO service. There is also included a change to the refresh-token service as it became clear with the new pipeline that it was making a request to SSO before knowing if that request was actually needed (i.e. before client credentials have been checked).

## 0.21.3 ([#57](https://git.mobcastdev.com/Zuul/auth-service/pull/57) 2014-09-16 11:24:43)

Move more settings into reference.conf

### Improvements

Move more settings from `application.conf` into `reference.conf`

## 0.21.2 ([#56](https://git.mobcastdev.com/Zuul/auth-service/pull/56) 2014-09-15 16:36:08)

CP-1837 Fix serialization when sending SSO data containing non-ascii characters

### Bugfix

This patch adds some tests around unicode character handing and fixes an issue where SSO was receiving incorrectly encoded data.

## 0.21.1 ([#55](https://git.mobcastdev.com/Zuul/auth-service/pull/55) 2014-09-15 13:49:45)

Improve configuration for deployment

This patch moves as much of the configuration out of application.conf as possible and either puts it into reference.conf where possible or just deletes it if it’s redundant or duplicated.

## 0.21.0 ([#52](https://git.mobcastdev.com/Zuul/auth-service/pull/52) 2014-09-12 13:11:17)

Implement role support

### New feature

* Introduce roles entities for Slick
* Introduce roles in the responses from the API
* Upgrade common-spray-auth to support role-checking

## 0.20.1 ([#51](https://git.mobcastdev.com/Zuul/auth-service/pull/51) 2014-09-11 14:46:37)

CP-1826 Fix failure for unelevated token and unify TokenStatus with SessionStatus

### Bugfix

* Fix failure on `GET /session` when the SSO token is unelevated
* Unify `TokenStatus` and `SessionStatus` 

## 0.20.0 ([#47](https://git.mobcastdev.com/Zuul/auth-service/pull/47) 2014-09-09 10:13:31)

Introduce real GeoIP checking

### New Features

* Update GeoIP submodule with the latest file from MaxMind
* Implement GeoIP checking on registration and some tests
* Implement test for terms and conditions and fix an issue on that

## 0.19.0 ([#48](https://git.mobcastdev.com/Zuul/auth-service/pull/48) 2014-09-09 13:13:32)

Added SSO elevation support

### New features

- Now uses the SSO session info endpoint to check elevation status of requests where required.
- The correct elevation levels are now applied to the API endpoints.

### Bug fixes

- `Authorization` header challenges are now constructed correctly.

### Improvements

- API test timeout is now 3 seconds to prevent spurious failures when setup takes a while.

## 0.18.2 ([#49](https://git.mobcastdev.com/Zuul/auth-service/pull/49) 2014-09-09 16:36:02)

Fix name length

### Bugfix

* Increase the length of first_name and last_name on the users table to match SSO limitations.


## 0.18.1 ([#46](https://git.mobcastdev.com/Zuul/auth-service/pull/46) 2014-09-08 13:48:38)

Update dependencies

### Bug fixes

- Dependencies updated to include a later version of common-config that
ensures substitutions in configuration are resolved before using it.

## 0.18.0 ([#45](https://git.mobcastdev.com/Zuul/auth-service/pull/45) 2014-09-05 16:42:42)

Added health check endpoints

### New features

- Now responds to the standard health check endpoints, although there
aren’t any proper health checks at the moment.

## 0.17.6 ([#44](https://git.mobcastdev.com/Zuul/auth-service/pull/44) 2014-09-05 15:10:09)

Strict check if SSO client expectations are met

### Improvements

* Introduce more strictness so that a test fails also if an expected SSO call has not been performed
* Remove useless code
* Re-enable parallel test execution; as strange as it may seem, in the current test structure it seems to be working fine

## 0.17.5 ([#43](https://git.mobcastdev.com/Zuul/auth-service/pull/43) 2014-09-05 12:29:05)

Update Scala & Optimize test environments

### Improvements

* Update to Scala 2.11.2
* Simplify tests by unifying all test environments
* Marginally quicker execution time (circa 20% quicker on development box for a full test run)
* Reduce resource leaks in tests (from 792/801 live/peak threads to 22/35 on a full test run)


## 0.17.4 ([#42](https://git.mobcastdev.com/Zuul/auth-service/pull/42) 2014-09-04 12:15:03)

Remove project code in favour of common libraries

### Improvements

* Remove slick database abstraction code and use common-slick instead
* Remove code reading `FiniteDuration` from Typesafe Config and use common-config instead

## 0.17.3 ([#40](https://git.mobcastdev.com/Zuul/auth-service/pull/40) 2014-09-02 16:07:41)

Execution contexts and connection pooling

### Improvements

In this PR I split the one execution context we were using into multiple of them so that heavy load on one part of the application won't block another part of it. The execution contexts I introduced are all `ForkJoinPool` instances with default configuration; this is just to get started with it and we can tune the details for each execution context as we prefer anyway.

I also introduced a connection pooling library: HikariCP. I had some experience with c3p0 and I did read some article about DHCP, c3p0, tomcat jdbc pool and BoneCP; in the and I chose this library because of its simplicity (the smaller code-base overall), its lock-free design and because it doesn't seem to have any negative comment around (e.g. c3p0 seems to be quite deadlock-prone and hard to tune). I am open to discussion aroud this choice anyway.

An interesting article on some connection pools: http://blog.trustiv.co.uk/2014/06/battle-connection-pools

## 0.17.2 ([#39](https://git.mobcastdev.com/Zuul/auth-service/pull/39) 2014-09-01 17:26:08)

Make things more configurable

### Improvements

In this PR I went over all the TODOs in the code that were about making things configurable and I made them configurable.

I also moved the keys needed for the `TokenBuilder` in the classpath resources so that tests can be run in any server and I solved that glitch that was causing tests to fail at the first run and pass at the second one. I have only a doubt: should we move in the classpath resources the `client.auth.keysDir` value for tests?

## 0.17.1 ([#38](https://git.mobcastdev.com/Zuul/auth-service/pull/38) 2014-08-29 16:43:26)

Housekeeping - fix all SSO camel casing inconsistencies

### Improvements

Another big one: fixes all the mess I did with the SSO acronym and camel casing.

## 0.17.0 ([#37](https://git.mobcastdev.com/Zuul/auth-service/pull/37) 2014-08-29 14:29:28)

Implement password-reset scenarios

### New feature

This PR implements the remaining password reset scenarios and add tests all over them. The size of this PR went out of control a couple of commits ago, so :cookie: will be provided!

## 0.16.6 ([#36](https://git.mobcastdev.com/Zuul/auth-service/pull/36) 2014-08-29 13:17:54)

Added tests for listing clients

### Improvements

- Now has scala tests for listing clients
- Removed a lot of messy serialisation imports
- Made it easier to test unauthorised errors

## 0.16.5 ([#35](https://git.mobcastdev.com/Zuul/auth-service/pull/35) 2014-08-29 11:24:51)

Updated dependencies

### Improvements

- Updated library dependencies to the latest versions.
- Fixes the build which needs a later version of message schemas.

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
