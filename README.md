# Zuul authentication server

## Prerequisites

The authentication server requires Ruby 2.0.0 or later as it uses the AES-128-GCM encryption algorithm for tokens, which isn't supported in 1.9.x. Note that it does not run under JRuby as it uses ECDSA signatures which are not currently supported on that platform.

### OS X Only

- Install [RVM](https://rvm.io/)
- Make sure you're using Ruby 2.0.0 (if you `cd` to the root folder then RVM should automatically switch to 2.0.0)

### Windows Only

_Note: In theory this should work, but the SCrypt gem doesn't seem to build on Windows x64 under Ruby 2.0.0 so it doesn't appear to be possible to run it on Windows at the moment._

- Install Ruby 2.0.0 x64 from [RubyInstaller](http://rubyinstaller.org/downloads/)
- Install the appropriate DevKit from the same place. Note: The folder you extract it to is where it will live, so don't use the default location.
- Go to the DevKit folder and run `ruby dk.rb init`
- Review the config.yml file and make sure only Ruby 2.0.0 is in there (comment out any 1.9.x versions if they're there)
- Run `ruby dk.rb install`

## Installing dependencies

Ensure you have bundler installed, as it is used to load dependencies:

```
$ gem install bundler
```

Install the dependencies using it:

```
$ bundle
```

## Running the server

To run the server in dev mode, just launch your favourite Rack development middleware, e.g.:

```
$ shotgun
```

The server will run against a local SQLite3 database which is fine for developing against.

If you want to scale beyond one concurrent user then you'll need to use a different database. This can be configured using the `DATABASE_URL` environment variable. To set up the database use the active record migrations, e.g. to set up MySQL:

```
$ rake db:migrate DATABASE_URL=mysql://localhost:3306/zuul?user=admin&password=cheese
```

Then run the server with a similar connection URL (though probably using a different user who doesn't have permission to modify the database schema):

```
$ rackup DATABASE_URL=mysql://localhost:3306/zuul?user=zuul&password=cheese
```