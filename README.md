# Running the sample

Ensure you have bundler installed, as it is used to load dependencies:

    gem install bundler

Start the OAuth server:

    cd auth_server/
    rackup --port=9393

Start the resource server from another prompt:

    cd resource_server/
    rackup --port=9394

Run the native app emulator from a third prompt:

    cd device/
    ruby App.rb