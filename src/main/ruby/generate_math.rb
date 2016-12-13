# Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html

# Quick and dirty script to generate the arithmetic operation methods

## On primitive types (box/unbox)
OPS_SYMB = {
  :plus => '+',
  :minus => '-',
  :divide => '/',
  :times => '*',
  :modulo => '%'
}

BOOL_OPS = {
  :equals => '==',
  :notequals => '!=',
  :less => '<',
  :lessorequals => '<=',
  :more => '>',
  :moreorequals => '>='
}

PRIM = {
  :Character => :char,
  :Integer => :int,
  :Long => :long,
  :Double => :double,
  :Float => :float
}

WEIGHT = {
  :Character => 1,
  :Integer => 2,
  :Long => 3,
  :Float => 4,
  :Double => 5
}

def generateMethod(out, op, symb, left, right, prim)
    puts "  public static #{out} #{op}(#{left} a, #{right} b) {"
    puts "    return ((#{prim}) a) #{symb} ((#{prim}) b);"
    puts "  }"
    puts
end

combinations =  PRIM.keys().combination(2).to_a
combinations += combinations.map { |pair| [pair[1], pair[0]] }
combinations += PRIM.keys().map { |v| [v, v] }
combinations.each do |pair|
  left = pair[0]
  right = pair[1]
  if WEIGHT[left] < WEIGHT[right]
      type = PRIM[right]
      out = right
  else
      type = PRIM[left]
      out = left
  end
  if type == :char
    out = :Integer
  end
  OPS_SYMB.each do |op, symb|
    generateMethod(out, op, symb, left, right, type)
  end
  BOOL_OPS.each do |op, symb|
    generateMethod(:Boolean, op, symb, left, right, type)
  end
end

# ..........................................................................
# On BigDecimal/BigInteger

INT_NUMBERS = [ :Integer, :Long, :BigInteger ]
REAL_NUMBERS = [ :Float, :Double ]
OPS_METH = {
  :plus => :add,
  :minus => :subtract,
  :times => :multiply,
  :divide => :divide,
  :modulo => :remainder
}

CONVERTERS = {
  :BigDecimal => -> (arg, type) { case type
    when :BigDecimal then arg
    else "new BigDecimal(#{arg})"
    end },

  :BigInteger => -> (arg, type) { case type
    when :BigInteger then arg
    when :BigDecimal then "#{arg}.toBigInteger()"
    else "BigInteger.valueOf(#{arg}.longValue())"
  end }
}

def generateComparison(op, symb, left, right, convert)
  puts "  public static Boolean #{op}(#{left} a, #{right} b) {"
  puts "    return (#{convert.("a", left)}).compareTo(#{convert.("b", right)}) #{symb} 0;"
  puts "  }"
  puts
end

def generateMathMethod(out, op, meth, left, right, convert)
  puts "  public static #{out} #{op}(#{left} a, #{right} b) {"
  puts "    return (#{convert.("a", left)}).#{meth}(#{convert.("b", right)});"
  puts "  }"
  puts
end

def generateOperators(numbers, the_type, output)
  the_conversion = CONVERTERS[output]
  numbers.each do |type|
    BOOL_OPS.each do |op, symb|
      generateComparison(op, symb, the_type, type, the_conversion)
      if type != the_type
        generateComparison(op, symb, type, the_type, the_conversion)
      end
    end
    OPS_METH.each do |op, meth|
      generateMathMethod(output, op, meth, the_type, type, the_conversion)
      if type != the_type
        generateMathMethod(output, op, meth, type, the_type, the_conversion)
      end
    end
  end
end

puts "  // ....................................................."
generateOperators(INT_NUMBERS, :BigDecimal, :BigDecimal)
generateOperators(REAL_NUMBERS + [ :BigDecimal ], :BigDecimal, :BigDecimal)
generateOperators(INT_NUMBERS, :BigInteger, :BigInteger)
generateOperators(REAL_NUMBERS, :BigInteger, :BigDecimal)

