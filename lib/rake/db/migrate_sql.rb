####################################################################
# ActiveRecord IO Mode plugin. Allows you to set the query statements
# that  ActiveRecord generates to an IO object (or anything that 
# supports the method <<).
#
# An initial connection to the database needs to occur. ActiveRecord
# has to establish a connection.
#
# See ActiveRecord::Base.send_sql_to for more information.
#
# = AUTHORS
# * Mark VanHolstyn (mvanholstyn@mktec.com)
# * Zach Dennis (zdennis@mktec.com)
# 
# = VERSION
# * 0.0.1
#
# = LICENSE (MIT style)
# Copyright (c) 2006 Market Technologies Inc. http://www.mktec.com
#
# Permission is hereby granted, free of charge, to any person obtaining 
# a copy of this software and associated documentation files 
# (the "Software"), to deal in the Software without restriction, 
# including without limitation the rights to use, copy, modify, merge, 
# publish, distribute, sublicense, and/or sell copies of the Software, 
# and to permit persons to whom the Software is furnished to do so, 
# subject to the following conditions: 
# 
# The above copyright notice and this permission notice shall be 
# included in all copies or substantial portions of the Software. 
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
# IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
# CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
# TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
# SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE
# 
# Except as contained in this notice, the name(s) of the above copyright 
# holders shall not be used in advertising or otherwise to promote the
# sale, use or other dealings in this Software without prior written 
# authorization.
####################################################################
module ActiveRecord

  class Base
  
    ########################################################################
    # Redirects all generated SQL statements. The passed in +arg+ can be a
    # Symbol; :db, :stdout or :stderr. The passed in +arg+ could also be
    # any object which responds to the method << . For example: Arrays,
    # Strings, StringIO objects, IO objects, etc.
    #
    # === Parameter
    #  * +arg+ - this can be a symbol, or an object which responds to <<
    #
    # === Examples
    # # send sql statements to the database, this is default ActiveRecord behavior
    # ActiveRecord::Base.send_sql_to :db
    #
    # # send sql statements to an Array
    # arr = []
    # ActiveRecord::Base.send_sql_to arr
    #
    # # send sql statements to a File
    # f = File.new 'sql.out', 'w'
    # ActiveRecord::Base.send_sql_to f
    #
    # # send sql statements to standard out
    # ActiveRecord::Base.send_sql_to :stdout
    #
    # # send sql statements to standard error
    # ActiveRecord::Base.send_sql_to :stderr
    #
    # # at any point to switch back to sending sql to the database 
    # ActiveRecord::Base.send_sql_to :db
    #
    # === Returns
    #  * true if successful, otherwise an exception is raised
    #
    # === Exceptions
    #  * ArgumentError - if the passed in argument is not an object \
    #    which responds to the method << or if it is a Symbol, but
    #    is not a Symbol that this method can handle.
    #
    # === See
    #  * ActiveRecord::Base.send_sql_to_with_symbol
    ########################################################################
    def self.send_sql_to arg
      if arg.is_a? Symbol
        ActiveRecord::Base.send_sql_to_with_symbol arg
      else
        if arg.respond_to? :<<  
          ActiveRecord::Base.connection.send_sql_to arg
        else
          error_msg = "Argument expected to respond to <<!"
          raise ArgumentError.new( error_msg )
        end
      end
      true
    end
    
    ########################################################################
    # Helper for send_sql_to which handles the symbol mappings for
    # :db, :stdout and :stderr
    #
    # === Exceptions
    #  * ArgumentError if the passed in argument is not a Symbol or \
    #    is not a symbol handled by this method.
    ########################################################################
    def self.send_sql_to_with_symbol arg
      case arg
        when :db
          ActiveRecord::Base.connection.db_mode
        when :stdout
          ActiveRecord::Base.connection.send_sql_to STDOUT
        when :stderr
          ActiveRecord::Base.connection.send_sql_to STDERR
        else
          error_msg = "When receiving a Symbol, expecting " +
            ":db, :stdout or :stderr, but received #{arg}"
          raise ArgumentError.new( error_msg )
      end
    end
  
  end

  module ConnectionAdapters
    class AbstractAdapter
    
      ########################################################################
      # Dummy object to mimic an empty set of results as given from the
      # database adapter. 
      # 
      # === MySQLAdapter
      # Uses a custom object which listens to the following methods:
      #  * each
      #  * free
      #
      # === PostgreSQLAdapter
      #  * Appears to be an array. 
      ########################################################################
      class ReturnResult < Array
        # Override to support Mysql's query results
        def each_hash &block
          each &block
        end
        
        # Override to support Mysql's query results. This doesn't
        # need to give a return value
        def free; end
      end
            
      ########################################################################
      # This method essentially "turns off" the database. This is done by 
      # aliasing the "execute" method and replacing it with our own. All 
      # queries that are run are added to the passed in io via "<<".
      #
      # === Parameters
      # * io - object to send all sql queries to. Must respond to "<<"
      ########################################################################
      def send_sql_to io
        @output_io = io
        str =<<-'EOT'
          alias :db_execute :execute
          alias :execute :io_execute
        EOT
        instance_eval str
      end
      
      ########################################################################
      # This method "turns on" the database. This is done by aliasing our 
      # execute back and aliasing the origional execute method back to 
      # execute. This forces all queries to not longer be logged, but to be 
      # passed on to the database.
      ########################################################################
      def db_mode
        str =<<-'EOT'
          alias :io_execute :execute
          alias :execute :db_execute
        EOT
        instance_eval str
      end
            
      ########################################################################
      # This method is the one that will replace the original "execute" method
      # used to pass sql statments to the database. When put into send_sql_to, 
      # this will be aliased to "execute"
      #
      # === Parameters
      # * sql - the sql to be logged to our object
      ########################################################################
      def io_execute(sql, name = nil, retries = 2)
        @output_io << sql.strip.gsub( /\s+/, ' ' ) + ";\n"
        ReturnResult.new
      end
      
    end 
  end
end

