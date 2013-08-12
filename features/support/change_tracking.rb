class Object

  # Defines attributes with change tracking, so for an attribute 'name' the following
  # methods would be defined:
  #
  # - `name` - Gets the attribute value.
  # - `name=` - Sets the attribute value.
  # - `name_changed?` - Gets a value indicating whether the attribute has been changed.
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
      define_method "#{name}=" do |val|
        if instance_variable_defined?("@#{name}")
          instance_variable_set("@#{name}_changed", val != instance_variable_get("@#{name}"))
        end
        instance_variable_set("@#{name}", val)
      end
    end
  end


end