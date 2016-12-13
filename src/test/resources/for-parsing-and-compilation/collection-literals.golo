module CollectionLiterals

function tuple = -> [1, 2, 3, ["a", "b"]]

function funky_data = -> map[
  ["foo", "bar"],
  ["plop", set[1, 2, 3, 4, 5]],
  ["mrbean", map[
    ["name", "Mr Bean"],
    ["email", "bean@outlook.com"]
  ]]
]

function with_comments = -> list[

  # a first element
  "foo",

  # another element
  "bar",

  # and the last one
  "baz"
]


function comprehension = -> list[ [a, b] foreach a in list["a", "b"] foreach b in range(0, 10) when b % 2 == 0 ]

# function comprehension = -> list[
#
#   # we create a couple
#   [a, b]
#
#   # whose first element is taken from...
#   foreach a
#
#     # ...this list
#     in list["a", "b"]
#
#   # and the second is an even integer
#   foreach b
#
#     # we create a range of integers
#     in range(0, 10)
#
#     # and filter it to keep only the even ones
#     when b % 2 == 0
#
# # and we're done!
# ]
