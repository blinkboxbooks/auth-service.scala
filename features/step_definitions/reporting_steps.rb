Then(/^a user (registration|update) message is sent$/) do |message_type|
  expect(Blinkbox::Zuul::Server::Reporting.sent_messages.size).to be > 0

  case message_type
  when "registration"
    #
  when "update"
    #
  end
end

Then(/^it contains the user's (registration|update) details$/) do |message_type|
  @message = Nokogiri::XML(Blinkbox::Zuul::Server::Reporting.sent_messages.pop)

  case message_type
  when "registration"
    reporting_message_value("/e:userCreated/e:timestamp") do |text|
      expect(text).to eq("013-12-30T19:15:23")
    end
    reporting_message_value("/e:userCreated/e:user/e:username") do |text|
      expect(text).to eq(@me.username)
    end
    reporting_message_value("/e:userCreated/e:user/e:firstName") do |text|
      expect(text).to eq(@me.first_name)
    end
    reporting_message_value("/e:userCreated/e:user/e:lastName") do |text|
      expect(text).to eq(@me.last_name)
    end
    reporting_message_value("/e:userCreated/e:user/e:allowMarketingCommunications") do |text|
      expect(text).to eq(@me.allow_marketing_communications)
    end
  when "update"
    reporting_message_value("/e:userUpdated/e:timestamp") do |text|
      expect(text).to eq("013-12-30T19:15:23")
    end
    reporting_message_value("/e:userUpdated/e:newUser/e:username") do |text|
      expect(text).to eq(@me.username)
    end
    reporting_message_value("/e:userUpdated/e:newUser/e:firstName") do |text|
      expect(text).to eq(@me.first_name)
    end
    reporting_message_value("/e:userUpdated/e:newUser/e:lastName") do |text|
      expect(text).to eq(@me.last_name)
    end
    reporting_message_value("/e:userUpdated/e:newUser/e:allowMarketingCommunications") do |text|
      expect(text).to eq(@me.allow_marketing_communications)
    end
  end
end

Then(/^a client (registration|update|deregistration) message is sent$/) do |message_type|
  pending
end

Then(/^it contains the client's (registration|update|deregistration) details$/) do |message_type|
  pending
end

def reporting_message_value(xpath)
  elem = @message.at_xpath(xpath, e: "http://schemas.blinkboxbooks.com/events/zuul/v1")
  expect(elem).to_not be_nil
  if block_given?
    yield elem.text
  else
    elem.text
  end
end
