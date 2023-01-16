
input_range_x = range(-5,5 +1)
input_range_y = range(-5,5 +1)

def formula(x,y):
  return (x + 5) * 11 + y + 5

for i in input_range_x:
  print("{", end="")
  for j in input_range_y:
    print(str(formula(i,j)) + ", ", end="")
  print("},")