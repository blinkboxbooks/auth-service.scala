require "ipaddress"

unless IPAddress::IPv4.respond_to?(:loopback?)
  class IPAddress::IPv4
    LOOPBACK = IPAddress::IPv4.new("127.0.0.0/8") unless defined?(LOOPBACK)
    def loopback?
      LOOPBACK.include?(self)
    end
  end
end