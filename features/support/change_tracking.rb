class Object

  # Defines attributes with change tracking, so for an attribute 'name' the following
  # methods would be defined:
  #
  # - `name` - Gets the attribute value.
  # - `name=` - Sets the attribute value.
  # - `name_changed?` - Gets a value indicating whether the attribute has been changed.
  #
  # When a value is changed, if the class implements a `after_name_changed` method then
  # it will be called with the old value and the new value as arguments, respectively.
  #
  # Note that only changes made via the accessors are tracked automatically; if the 
  # underlying attribute value is changed directly then the change is not tracked.
  #
  # @param [Symbol] Symbol(s) representing the attribute name(s).
  def tracked_attr_accessor(*names)
    names.each do |name|
      define_method name do
        instance_variable_get("@#{name}")
      end
      define_method "#{name}_changed?" do
        instance_variable_defined?("@#{name}_changed") && instance_variable_get("@#{name}_changed")
      end
      define_method "#{name}=" do |new_value|
        if instance_variable_defined?("@#{name}")
          old_value = instance_variable_get("@#{name}")
          instance_variable_set("@#{name}_changed", new_value != instance_variable_get("@#{name}"))
          if self.respond_to?("after_#{name}_changed")
            self.send("after_#{name}_changed", old_value, new_value)
          end
        end
        instance_variable_set("@#{name}", new_value)
      end
    end
  end


end