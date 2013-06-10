require "mechanize"
require_relative "./common_transforms"
require_relative "./step_helpers"

Before do 
  @agent = Mechanize.new
end