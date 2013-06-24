# Pre-Setup (Windows)

- Install Ruby 1.9.3 from [RubyInstaller](http://rubyinstaller.org/downloads/)
- Install the appropriate DevKit (the TDM one) from the same place. Note: The folder you extract it to is where it will live.
- Go to the DevKit folder and run `ruby dk.rb init`
- Review the config.yml file
- Run `ruby dk.rb install`

# Installing dependencies

Ensure you have bundler installed, as it is used to load dependencies:

    gem install bundler

The first time you run each of the apps you can install the dependencies from their folder using:

    bundle install

# Running the apps

Start the OAuth server:

    cd auth_server/
    rackup --port=9393

Start the resource server from another prompt:

    cd resource_server/
    rackup --port=9394

Run the native app emulator from a third prompt - this gets a token from the OAuth server and uses it in a request to the resource server.

    cd device/
    ruby App.rb