#include <stdint.h>
#include "functions.h"
int global_variable = 3;
const char const_value[] = "Hello, World";
volatile char char_len = sizeof(char);
volatile short short_len = sizeof(short);
volatile int int_len = sizeof(int);
volatile long long_len = sizeof(long);
volatile int32_t int32_len = sizeof(int32_t);


int add (int a) {
  static int static_variable = 0;
  static_variable++;
  return a + global_variable;
}

char get_hello_char() {
  return const_value[global_variable];
}

int get_size_sum() {
  return char_len + short_len + int_len + long_len;
}

int main (void) {
  int a = 10;
  a = a && get_hello_char();
  a = a + exampleFunction();
  return add(a);
}
