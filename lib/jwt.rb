require 'openssl'
require 'base64'
require 'json'

module JWT

  # creates a signed token
  def self.signed_token(payload, private_key, header_fields)
    alg = 'RS256'

    # create the header
    header = JSON.generate({ alg: alg }.merge(header_fields))
    encoded_header = base64_encode(header)

    # sign the header and payload
    digest = OpenSSL::Digest::SHA256.new
    secured_input = [encoded_header, payload].join('.')
    signature = private_key.sign(digest, secured_input)
    encoded_signature = base64_encode(signature)

    # create the jws token
    [secured_input, encoded_signature].join('.')
  end

  # creates an encrypted token
  def self.encrypted_token(payload, public_key, header_fields)
    alg = "RSA1_5"
    enc = "A128CBC+HS256"

    # create header
    header = JSON.generate({ alg: alg, enc: enc }.merge(header_fields))
    encoded_header = base64_encode(header)

    # generate content master key and initialization vector
    aes = OpenSSL::Cipher::AES128.new(:CBC)
    aes.encrypt
    content_master_key = aes.random_key
    iv = aes.random_iv

    # encrypt content master key with recipient's public key
    encrypted_key = public_key.public_encrypt(content_master_key)
    encoded_encrypted_key = base64_encode(encrypted_key)

    # derive keys
    content_encryption_key = derive_content_key('Encryption', content_master_key, enc, 128)
    content_integrity_key = derive_content_key('Integrity', content_master_key, enc, 256)

    # encode initialization vector
    encoded_iv = base64_encode(iv)

    # encrypt jws token
    ciphertext = aes.update(payload) + aes.final
    encoded_ciphertext = base64_encode(ciphertext)

    # compute integrity value
    secured_input_value = [encoded_header, encoded_encrypted_key, encoded_iv, encoded_ciphertext].join('.')
    integrity_value = OpenSSL::Digest.digest('SHA256', secured_input_value)
    encoded_integrity_value = base64_encode(integrity_value)

    # create token
    [secured_input_value, encoded_integrity_value].join('.')
  end

  # decodes a signed token
  def self.decode_token(signed_token, public_key)
    # decode token parts
    parts = signed_token.split('.')
    throw ArgumentError.new('Invalid token format') unless parts.length == 3
    begin
      header = JSON.parse(base64_decode(parts[0]))
      payload = JSON.parse(base64_decode(parts[1]))
      signature = base64_decode(parts[2])
    rescue
      throw ArgumentError.new('Invalid token encoding.')
    end

    # verify signature
    alg = header['alg']
    throw ArgumentError.new('Unsupported signing method.') unless alg == 'RS256'
    digest = OpenSSL::Digest::SHA256.new
    secured_input = parts.take(2).join('.')
    throw ArgumentError.new('Invalid signature.') unless public_key.verify(digest, signature, secured_input)

    # return the payload
    payload
  end

  def self.decrypt_token(encrypted_token, private_key)
    # decode token parts
    parts = encrypted_token.split('.')
    throw ArgumentError.new('Invalid token format.') unless parts.length == 5
    begin
      header = JSON.parse(base64_decode(parts[0]))
      encrypted_key = base64_decode(parts[1])
      iv = base64_decode(parts[2])
      ciphertext = base64_decode(parts[3])
      integrity_value = base64_decode(parts[4])
    rescue
      throw ArgumentError.new('Invalid token encoding.')
    end

    # check integrity value
    alg = header['alg']
    enc = header['enc']
    throw ArgumentError.new('Unsupported encryption method.') unless alg == "RSA1_5" && enc == "A128CBC+HS256"
    secured_input_value = parts.take(4).join('.')
    computed_integrity_value = OpenSSL::Digest.digest('SHA256', secured_input_value)
    throw ArgumentError.new('Invalid signature.') unless integrity_value == computed_integrity_value

    # decrypt key
    content_master_key = private_key.private_decrypt(encrypted_key)

    # decrypt payload
    aes = OpenSSL::Cipher::AES128.new(:CBC)
    aes.decrypt
    aes.key = content_master_key
    aes.iv = iv
    aes.update(ciphertext) + aes.final
  end

  def self.base64_encode(s)
    Base64.urlsafe_encode64(s).gsub(%r{=+$}, '')
  end

  def self.base64_decode(s)
    padding_length = (4 - (s.length % 4)) % 4
    padding = '=' * padding_length
    Base64.urlsafe_decode64(s + padding)
  end

  private

  def self.derive_content_key(label, content_master_key, enc, size)
    round_number = [1].pack('N')
    output_size = [size].pack('N')
    enc_bytes = enc.encode('utf-8').bytes.to_a.pack('C*')
    epu = epv = [0].pack('N')
    label_bytes = label.encode('us-ascii').bytes.to_a.pack('C*')
    hash_input = round_number + content_master_key + output_size + enc_bytes + epu + epv + label_bytes
    hash = OpenSSL::Digest.digest('SHA256', hash_input)
    hash[0..((size / 8) - 1)]
  end

end

if __FILE__ == $0

  # create payload
  issued_at = Time.now
  claims = JSON.generate({
    iss: 'blinkboxbooks',                           # issuer
    aud: 'blinkboxbooks',                           # audience
    sub: 'urn:blinkboxbooks:id:user:18273',         # user id
    iat: issued_at.to_i,                            # issued at
    exp: (issued_at + 3600).to_i,                   # expires
    jti: '15b29d70-4e54-4286-be17-e9ba97d45194',    # token identifier
    bbb_dcc: 'GB',             # detected country code
    bbb_rcc: 'GB',             # registered country code
    bbb_rol: [1, 2, 8, 13],    # user roles
    bbb_did: 716352,           # device identifier
    bbb_dcl: 27                # device class
  })
  encoded_claims = JWT.base64_encode(claims)

  # sign and encrypt
  jws_key = OpenSSL::PKey::RSA.new(File.read('../auth_server/keys/auth_server_priv.pem'))
  jws_token = JWT.signed_token(encoded_claims, jws_key, { kid: 'bbb-as-1' })
  jwe_key = OpenSSL::PKey::RSA.new(File.read('../auth_server/keys/resource_server_pub.pem'))
  jwe_token = JWT.encrypted_token(jws_token, jwe_key, { cty: 'JWT', kid: 'bbb-rs-1' })

  # puts "claims [#{claims.length} bytes] #{claims}"
  # puts "encoded_claims [#{encoded_claims.length} bytes] #{encoded_claims}"
  # puts "jws_token [#{jws_token.length} bytes] #{jws_token}"
  # puts "jwe_token [#{jwe_token.length} bytes] #{jwe_token}"

  jwe_decryption_key = OpenSSL::PKey::RSA.new(File.read('../resource_server/keys/resource_server_priv.pem'))
  rs_jws_token = JWT.decrypt_token(jwe_token, jwe_decryption_key)
  jws_verification_key = OpenSSL::PKey::RSA.new(File.read('../resource_server/keys/auth_server_pub.pem'))
  rs_claims = JWT.decode_token(rs_jws_token, jws_verification_key)

  puts rs_claims

end
