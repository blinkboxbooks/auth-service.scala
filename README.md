# Installing dependencies

Ensure you have bundler installed, as it is used to load dependencies:

    gem install bundler

The first time you run each of the apps you can install the dependencies from their folder using:

    bundle install

# Running the apps

Start the OAuth server:

    cd auth_server/
    bundle install
    rackup --port=9393

Start the resource server from another prompt:

    cd resource_server/
    bundle install
    rackup --port=9394

Run the native app emulator from a third prompt - this gets a token from the OAuth server and uses it in a request to the resource server.

    cd device/
    bundle install
    ruby App.rb