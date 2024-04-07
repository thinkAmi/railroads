require 'base64'

file_paths = Dir.glob('*')
file_paths.each do |file_path|
  file_name = File.basename(file_path)

  # Do not output Base64-encoded strings for running Ruby scripts and example files
  next if file_name == File.basename(__FILE__)
  next if File.extname(file_name) == '.example'

  file_data = File.read(file_path)

  # Do not add newlines when Base64 encoded
  encoded_data = Base64.strict_encode64(file_data)

  puts "File Name: #{file_name}"
  puts "Encoded Data:"
  puts encoded_data
  puts "\n\n\n"
end
