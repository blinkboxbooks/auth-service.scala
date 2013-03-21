require 'openssl'
require 'base64'
require 'json'

# patch the Base64 module to add urlsafe methods that don't have the equals padding at the end
module Base64
  def self.urlsafe_encode64_nopad(s)
    Base64.urlsafe_encode64(s).gsub(%r{=+$}, '')
  end
  def self.urlsafe_decode64_nopad(s)
    padding_length = (4 - (s.length % 4)) % 4
    padding = '=' * padding_length
    Base64.urlsafe_encode64(s + padding)
  end
end

def derive_content_key(label, content_master_key, enc, size)
  round_number = [1].pack('N')
  output_size = [size].pack('N')
  enc_bytes = enc.encode('utf-8').bytes.to_a.pack('C*')
  epu = epv = [0].pack('N')
  label_bytes = label.encode('us-ascii').bytes.to_a.pack('C*')
  hash_input = round_number + content_master_key + output_size + enc_bytes + epu + epv + label_bytes
  hash = OpenSSL::Digest.digest('SHA256', hash_input) # TODO: SHA256 shouldn't be hard-coded here!
  hash[0..((size / 8) - 1)]
end

# ----------------------------------------------------------------------------------------------------------
# JWT
# ----------------------------------------------------------------------------------------------------------

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
encoded_claims = Base64.urlsafe_encode64_nopad(claims)

# ----------------------------------------------------------------------------------------------------------
# JWS
# ----------------------------------------------------------------------------------------------------------

# create header
jws_header = JSON.generate({ alg: 'RS256', kid: 'bbb-svcs-1' })
encoded_jws_header = Base64.urlsafe_encode64_nopad(jws_header)

# create signature
jws_rsa = OpenSSL::PKey::RSA.new(File.read('../auth_server/auth_server_priv.pem'))
jws_sha = OpenSSL::Digest::SHA256.new
jws_secured_input = [encoded_jws_header, encoded_claims].join('.')
jws_signature = jws_rsa.sign(jws_sha, jws_secured_input)
encoded_jws_signature = Base64.urlsafe_encode64_nopad(jws_signature)

# create token
jws_token = [encoded_jws_header, encoded_claims, encoded_jws_signature].join('.')

p jws_token.length
p jws_token

# ----------------------------------------------------------------------------------------------------------
# JWE
# ----------------------------------------------------------------------------------------------------------

# create header
alg = "RSA1_5"
enc = "A128CBC+HS256"
header = JSON.generate({ alg: alg, enc: enc, kid: 'bbb-auth-1', cty: 'JWT' })
encoded_header = Base64.urlsafe_encode64_nopad(header)

# generate content master key and initialization vector
aes = OpenSSL::Cipher::AES128.new(:CBC)
aes.encrypt
content_master_key = aes.random_key
iv = aes.random_iv

# encrypt content master key with recipient's public key
rsa = OpenSSL::PKey::RSA.new(File.read('../auth_server/resource_server_pub.pem'))
encrypted_key = rsa.public_encrypt(content_master_key)
encoded_encrypted_key = Base64.urlsafe_encode64_nopad(encrypted_key)

# derive keys
content_encryption_key = derive_content_key('Encryption', content_master_key, enc, 128)
content_integrity_key = derive_content_key('Integrity', content_master_key, enc, 256)

# encode initialization vector
encoded_iv = Base64.urlsafe_encode64_nopad(iv)

# encrypt jws token
ciphertext = aes.update(jws_token) + aes.final
encoded_ciphertext = Base64.urlsafe_encode64_nopad(ciphertext)

# compute integrity value
secured_input_value = [encoded_header, encoded_encrypted_key, encoded_ciphertext].join('.')
integrity_value = OpenSSL::Digest.digest('SHA256', secured_input_value)
encoded_integrity_value = Base64.urlsafe_encode64_nopad(integrity_value)

# create token
token = [encoded_header, encoded_encrypted_key, encoded_iv, encoded_ciphertext, encoded_integrity_value].join('.')
p token.length
p token
