# Zuul authentication server

## Prerequisites

The authentication server requires Ruby 2.0.0 or later as it uses the AES-128-GCM encryption algorithm for tokens, which isn't supported in 1.9.x. Note that it does not run under JRuby as it uses ECDSA signatures which are not currently supported on that platform.

### CentOS only

On the CentOS image, Ruby and OpenSSL should be already set up correctly.

If you're planning on doing development using SQLite then you'll need to install it as a pre-requisite though. To do this run:

```
$ yum install sqlite-devel
```

### OS X only

- Install [RVM](https://rvm.io/)
- Make sure you're using Ruby 2.0.0 (if you `cd` to the root folder then RVM should automatically switch to 2.0.0)

### Windows only

_Note: In theory this should work, but the SCrypt gem doesn't seem to build on Windows x64 under Ruby 2.0.0 so it doesn't appear to be possible to run it on Windows at the moment. Feel free to have a crack at fixing this if you really want to make it work, otherwise I'd suggest using CentOS or OS X._

- Install Ruby 2.0.0 x64 from [RubyInstaller](http://rubyinstaller.org/downloads/)
- Install the appropriate DevKit from the same place. Note: The folder you extract it to is where it will live, so don't use the default location.
- Go to the DevKit folder and run `ruby dk.rb init`
- Review the config.yml file and make sure only Ruby 2.0.0 is in there (comment out any 1.9.x versions if they're there)
- Run `ruby dk.rb install`

### Checking the prerequisites

In a terminal window run:

```
$ ruby -v  
  #=> should print something with 2.0.0-p195 in it
$ irb
$> require "openssl"
  #=> should print true
$> OpenSSL::OPENSSL_VERSION
  #=> should print something with 1.0.1c or higher in it
$> OpenSSL::PKey::EC
  #=> should not print an error about this being undefined     
```

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

### In dev mode

To run the server in dev mode, just launch your favourite Rack development middleware, e.g.:

```
$ shotgun
```

(Note: you can use `rackup` but `shotgun` is a really handy server as it reloads your code changes automatically. If you haven't got it yet just run `gem install shotgun`.)

The server will run against a local SQLite3 database which is fine for developing against.

### In production mode

If you want to scale beyond one concurrent user then you'll need to use a different database. This can be configured using the `DATABASE_URL` environment variable. To set up the database use the active record migrations, e.g. to set up MySQL:

```
$ rake db:migrate DATABASE_URL=mysql://localhost:3306/zuul?user=admin&password=cheese
```

If you don't like running active record migrations and would rather run plain old SQL then set up a sacrificial database and use the `db:migrate_with_ddl` task instead, e.g.

```
$ rake db:migrate_with_ddl DATABASE_URL=mysql://localhost:3306/zuul?user=admin&password=cheese
```

This will output the SQL that was sent to the database in a file called "migration.sql" so you can run that on a different instance later. If you really want, you can set a different file name for the output, e.g.

```
$ rake db:migrate_with_ddl["my_file.sql"] DATABASE_URL=mysql://localhost:3306/zuul?user=admin&password=cheese
```

Once your database is set up, run the server with a similar connection URL (though probably using a different user who doesn't have permission to modify the database schema):

```
$ rackup DATABASE_URL=mysql://localhost:3306/zuul?user=zuul&password=cheese
```

## Running the tests

The tests are written using Cucumber, so to run them just run:

```
$ cucumber
```

The tests assume you're using Shotgun as your development server on your local machine, so will attempt to run against `http://localhost:9393/` by default. You can change this by specifying the `AUTH_SERVER` environment variable, e.g.

```
$ cucumber AUTH_SERVER=https://myserver:123/
```

They should all pass. If they don't, fix it or raise a bug.
