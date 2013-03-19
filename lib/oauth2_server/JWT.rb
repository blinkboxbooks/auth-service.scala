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
  hash = OpenSSL::Digest.digest('SHA256', hash_input)
  hash[0..((size / 8) - 1)]
end

# create header
alg = "RSA1_5"
enc = "A128CBC+HS256"
header = JSON.generate({ alg: alg, enc: enc, kid: 'bbb1' })
encoded_header = Base64.urlsafe_encode64_nopad(header)

# generate content master key and initialization vector
aes = OpenSSL::Cipher::AES256.new(:CBC)
aes.encrypt
content_master_key = aes.random_key
iv = aes.random_iv

# encrypt content master key with recipient's public key
rsa = OpenSSL::PKey::RSA.new(File.read('../../auth_server/resource_server_pub.pem'))
encrypted_key = rsa.public_encrypt(content_master_key)
encoded_encrypted_key = Base64.urlsafe_encode64_nopad(encrypted_key)

# derive keys
content_encryption_key = derive_content_key('Encryption', content_master_key, enc, 128)
content_integrity_key = derive_content_key('Integrity', content_master_key, enc, 256)

# encode initialization vector
encoded_iv = Base64.urlsafe_encode64_nopad(iv)

# encode text
plaintext = JSON.generate({
  sub: 173634,
  exp: (Time.now + 3600).to_i,
  'bbb/dcc' => 'GB',
  'bbb/rol' => [1, 3, 7, 4],
  'bbb/did' => 182749,
  'bbb/dty' => 18
})
ciphertext = aes.update(plaintext) + aes.final
encoded_ciphertext = Base64.urlsafe_encode64_nopad(ciphertext)

# compute integrity value
secured_input_value = "#{encoded_header}.#{encoded_encrypted_key}.#{encoded_ciphertext}"
integrity_value = OpenSSL::Digest.digest('SHA256', secured_input_value)
encoded_integrity_value = Base64.urlsafe_encode64_nopad(integrity_value)

# assembly final representation
token = "#{encoded_header}.#{encoded_encrypted_key}.#{encoded_iv}.#{encoded_ciphertext}.#{encoded_integrity_value}"
p token.length
p token

p Base64.urlsafe_encode64_nopad((0..400).to_a.pack('C*')).length