
def default_create_table_options
  adapter_name = ActiveRecord::Base.connection.adapter_name
  case adapter_name.downcase
  when "mysql", "mysql2"
    "CHARACTER SET utf8 COLLATE utf8_general_ci"
  when "sqlite"
    "COLLATE NOCASE"
  else
    warn "create_table options are not specified for adapter '#{adapter_name}'"
    ""
  end
end