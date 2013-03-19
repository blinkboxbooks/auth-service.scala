require 'base64'
require 'openssl'
require 'json'

module OAuth2Server

  # NOTE: Using JSON here just as an easy format, not a particularly space-efficient one
  class AccessToken

    attr_accessor :userId
    attr_accessor :issued
    attr_accessor :expires

    def initialize(token_string = nil)
      if token_string
        data = JSON.parse(token_string)
        @userId = data['userId']
        @issued = data['issued']
        @expires = data['expires']
      end
    end

    def to_s
      JSON.generate({ userId: @userId, issued: @issued, expires: @expires })
    end

  end

  class SymmetricToken

    # NOTE: These would not be stored in code in the real world!
    VALIDATION_KEY = Base64.decode64('fSw1/Dtt9Fd6yyIW/v48AR1m1wwPzz1kMM06MKjlGvK8IKsI+VChraAVBbWN 4Beuc9f2CXaWLDsDKVSJsz99AQ==')
    ENCRYPTION_KEY = Base64.decode64('ukZPkxdPHjCyKYUdeZefhFq0cTfkuwIRoYUbHB9dj1Y=')
 
    def self.decrypt_access_token(token_string)
      AccessToken.new(decrypt_token_string(token_string))
    end

    def self.encrypt_access_token(token)
      encrypt_token_string(token.to_s)
    end

    private

    def self.decrypt_token_string(token_string)
      encrypted_data = Base64.decode64(token_string)

      aes = OpenSSL::Cipher::AES256.new(:CBC)
      aes.decrypt
      aes.key = ENCRYPTION_KEY
      aes.iv = encrypted_data[0..15]
      validatable_data = aes.update(encrypted_data[16..-1]) + aes.final

      expected_hmac = validatable_data[0..19]
      decrypted_string = validatable_data[20..-1]
      actual_hmac = OpenSSL::HMAC.digest(OpenSSL::Digest::SHA1.new, VALIDATION_KEY, decrypted_string)
      throw ArgumentError.new('Invalid token.') unless actual_hmac == expected_hmac

      decrypted_string
    end

    def self.encrypt_token_string(token_string)
      hmac = OpenSSL::HMAC.digest(OpenSSL::Digest::SHA1.new, VALIDATION_KEY, token_string)
      validatable_data = hmac + token_string

      aes = OpenSSL::Cipher::AES256.new(:CBC)
      aes.encrypt
      aes.key = ENCRYPTION_KEY
      encryption_iv = aes.random_iv
      encrypted_data = aes.update(validatable_data) + aes.final

      Base64.encode64(encryption_iv + encrypted_data)
    end

  end

end