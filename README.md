# Authentication service

**To run the authentication service in production please provide the following option to the JVM to avoid DNS caching issues:**

```
-Dnetworkaddress.cache.ttl=60
```

This example sets the TTL for the DNS cache to 60 seconds; it is up to OPS to decide the value that would best suite our environment (or maybe even disable the cache by setting the TTL to 0).

# Beware!

**This part of the Readme is outdated as this repository contains the WIP for a new version of Zuul based on spray.**

# Zuul authentication server [![Build Status](http://teamcity01.mobcastdev.local:8111/app/rest/builds/buildType:%28id:Platform_ZuulServer_RakeBuild%29/statusIcon)](http://teamcity01.mobcastdev.local:8111/viewType.html?buildTypeId=Platform_ZuulServer_RakeBuild&guest=1)

An OAuth 2.0 based authentication server, supporting user registration and profile management, and client registration and management.

## Requirements

The authentication server requires Ruby 2.0.0 or later as it uses the AES/GCM encryption algorithm/mode for tokens, which isn't supported in 1.9.x. Note that it does not run under JRuby as it uses ECDSA signatures which are not currently supported on that platform.

You cannot run this server on Windows due to issues with Ruby versions and native gems. Use OS X or CentOS instead.

## Developer install on OS X

Install [RVM](https://rvm.io/) and make sure you're using Ruby 2.0.0 (if you `cd` to the root folder then RVM should automatically switch to 2.0.0 as it'll detect it from the Gemfile).

To check that your Ruby environment is suitable, run:

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

Also install [Brew](http://brew.sh/) and then use it to install MySQL and RabbitMQ:

```
$ brew install mysql
$ brew install rabbitmq
```

Ensure you have bundler installed, as it is used to load dependencies:

```
$ gem install bundler
```

For development or testing you can install all the dependencies using the `install` command:

```
$ bundle install
```

If you have cloned the code from git, then you'll need to ensure that the submodules are also initialised and up-to-date.

```
$ git submodule init
$ git submodule update
```

## Rig install on CentOS

On the CentOS image, the Ruby environment with OpenSSL should be already set up correctly. You can check this in the same way as the developer install.

To allow the MySQL gems to compile their native extensions, you need to download and install the [MySQL developer RPM package](http://dev.mysql.com/get/Downloads/MySQL-5.6/MySQL-devel-5.6.12-1.el6.x86_64.rpm/from/http://cdn.mysql.com/).

```
$ rpm -ivh /path/to/MySQL-devel-5.6.12-1.el6.x86_64.rpm
```

Ensure you have bundler installed, as it is used to load dependencies:

```
$ gem install bundler
```

Install the production dependencies by excluding the development and test groups:

```
$ bundle install --without development:test
```

If you have cloned the code from git, then you'll need to ensure that the submodules are also initialised and up-to-date.

```
$ git submodule init
$ git submodule update
```

## MySQL database creation

The database connection settings can be configured in the app.properties file by changing the `database_url` property. Note that this file is not committed to git, but you can clone one of the example files and edit it for your installation. An example connection setting is:

```
database_url = mysql://username:password@host:port/database
```

First create a database, e.g.

```
$ mysql -e "CREATE DATABASE zuul"
```

Then create a user and grant them permissions. For example the following creates a user called 'zuul' and grants the necessary permissions on the 'zuul' database:

```
$ mysql -e "CREATE USER zuul IDENTIFIED BY 'M0bc45T'"
$ mysql -e "GRANT SELECT, INSERT, UPDATE, DELETE ON zuul.* to 'zuul'@'localhost'"
```

To set up the database tables use the active record migrations:

```
$ rake db:migrate
```

If you don't like running active record migrations and would rather run plain old SQL then set up a sacrificial database and use the `db:migrate_with_ddl` task instead, e.g.

```
$ rake db:migrate_with_ddl
```

This will output the SQL that was sent to the database in a file called "migration.sql" so you can run that on a different instance later. If you really want, you can set a different file name for the output, e.g.

```
$ rake db:migrate_with_ddl["my_file.sql"]
```

## Running the server

The production server runs in Apache/Passenger and is not documented here in any detail. However, it still needs to follow the same process to configure the application.

To configure the app, select one of the example zuul.properties files and copy it to the real configuration file name, e.g.

```
$ cp zuul.properties.development zuul.properties
```

Once that is done, edit the file to ensure you have the correct settings. Most of them should be fine as they are, but you'll probably need to put the correct credentials in the `database_url` property, as described in setting up the database.

**Ensure that you're using a decent web server** such as Thin, because WEBrick will fall over in a massive heap as soon as you put any load on it.

To start the server in development/test mode using Thin run:

```
$ rackup -s thin --port 9393
```

## Running the tests

The tests are written using Cucumber. By default the tests will start an in-process instance of the auth server on IP address 127.0.0.1 and port 9393 so all you have to do is run:

```
$ cucumber
```

Any loopback IP address for the auth server will start an in-process instance by default, whereas any non-loopback address will run the tests against an existing (out-of-process) server. This should normally be what you want, but you can force the tests to run out-of-process against a server on a loopback address by specifying `IN_PROC=false` as an environment variable.

The reason that the tests run in-process by default is to reduce external dependencies on things like message queues and emailing services, and also because some of them are very slow (as in they take days to run). If you're going to run the tests out of process, you probably want to exclude the `@slow` and `@extremely_slow` tests unless you're actually working on them.

```
$ cucumber -t ~@slow -t ~@extremely_slow IN_PROC=false
```

To run against a different auth server you can specify the `AUTH_SERVER` environment variable, e.g.

```
$ cucumber -t ~@slow -t ~@extremely_slow AUTH_SERVER=https://myserver:123/
```

If you want to inspect what's going over the wire, then you can either use an HTTP debugging proxy such as [Charles](http://www.charlesproxy.com/) and specify the `PROXY_SERVER` environment variable, e.g.

```
$ cucumber -t ~@slow -t ~@extremely_slow PROXY_SERVER=http://localhost:8888/
```

The proxy server approach doesn't work with GeoIP tests as it needs to use an external proxy, so as an alternative you can specify the `DEBUG` environment variable, e.g.

```
$ cucumber -t ~@slow -t ~@extremely_slow DEBUG=true
```

Note that the `AUTH_SERVER`, `PROXY_SERVER` and `DEBUG` variables can be used together.

All of the tests should pass. If they don't, fix it or raise a bug.

## Updating GeoIP data files

The GeoIP data files are stored in a git submodule, [geoip-data](https://git.mobcastdev.com/Zuul/geoip-data) to ensure that this repo doesn't get too large.

To update the data files, update the GeoIP.dat file in the geoip-data repo with the latest country data file, and then run the following commands in your Zuul server directory:

```
$ git submodule init
$ git submodule update
```

## Advanced: Using SQLite instead of MySQL for development

Only use this option if you know what you're doing and why you need to be doing it. If you have to ask, don't use this option.

If you're only planning on doing development/testing against SQLite, without MySQL, you can use the `--without mysql` argument to bundler. Your connection string will need to look something like this:

    sqlite3:///db/zuul.db

If you need any more help than this, don't use this option.
