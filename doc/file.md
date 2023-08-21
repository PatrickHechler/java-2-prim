# Java-2-Prim
the [Constants](#constants) section defines Constants.    
the [Method call](#method-call) section describes how natives code has to invoke java code, how native code is invoked by java code and how java code invokes other java methods.    
the [Class Loading](#class-loading) section describes how classes are loaded and found    
the [Method excecution](#method-excecution) section describes how java methods are executed.    

## Constants
+ `ERR_JAVA_THROW` : `17` : `HEX-11`
    + this constant indicates that an java exception is currently thrown

## Method call
### JNI Argument
#### Java code calls
the _JNI-Env_ pointer is passed in the `X1F` register    
+ if a `native` method is invoked _pvm-java_ sets the `X1F` register to the _JNI-Env_ pointer after the native code returns
    + the native code __must__ not modify the _JNI-Env_ pointers target/content
+ otherwise the _JNI-Env_ pointer also has to be stored in the `X1F` register when the method returns

#### native code calls java code
_pvm-java_ saves the `X1F` register on the stack and overwrites it with the _JNI-Env_ pointer

### Arguments
if possible, the arguments are stored in the registers after the `X1F` register:
1. `X20` contains the first argument (`this` for non-static methods)
2. `X21` contains the second argument
3. ...

if this is not possible (there are more arguments then registers after the `X1F` register exist)
+ `X20` points to the arguments
+ `X20` was allocated with `INT_MEMORY_ALLOC` and is not needed anywhere else
    + this means that the method is can use `INT_MEMORY_REALLOC` to resize the array
    + this means also that the invoked method __must__ use the `INT_MEMORY_FREE` interrupt, once the block is no longer needed by the method

if there are no arguments the first method nothing needs to be done
### Return
#### Normal Return
if there is a return value it is stored in the `X00` register.

#### Return with an Exception being thrown
##### Native code
native code can use the `JNI-Env` to do the following exception handling related things:
+ throw an exception instance
    + _pvm-java_ then sets `ERRNO` to `ERR_JAVA_THROW` before it returns to the native code
+ ask/find out if there is an exception which is currently being thrown.
+ catch the exception instance which is currently being thrown
    + by doing so the native code also gets a reference to the exception instance
    + if `ERRNO` is still set to `ERR_JAVA_THROW` _pvm-java_ sets it to `0`

1. when native code returns,
2. no exception instance is currently being thrown and
3. `ERRNO` is set to a non-`0` and non-`ERR_JAVA_THROW` value then
4. _pvm-java_ automatically creates an exception instance and then imidiatly throws it.

native code can throw exceptions in two ways:
+ explicitly using the _JNI-Env_ to throw an exception instance
+ returning to java code with `ERRNO` set to a non-`0` and non-`ERR_JAVA_THROW` value
    + note that this only works, when no exception instance is already being thrown

##### Java Code
a reference to that exception is stored in the `X00` register and `ERRNO` is set to `ERR_JAVA_THROW`.    
when returning from native code _pvm-java_ __must__ set `X00` and `ERRNO`    
when returning from java code to native code _pvm-java_ __must__ save a reference to the exception instance from `X00` somewhere else, when `ERRNO` is set to `ERR_JAVA_THROW`

## Class Loading
classes are loaded/found using the `INT_LOAD_LIB` interrupt.    
all classes have to store the offset (as a 64-bit value) of their `<cinit>` method at the start of the file. (the offset is relative to the file-start)    
if a class has no `<cinit>` method the offset is instead set to `-1`    

## Method excecution
the [Local Variables](#local-variables) section describes how the local variables are stored during the execution of an java method.    
the [Operant Stack](#operant-stack) section describes how the translator 

### Local Variables
the first local variables are used like they was passed on method invokation (see [Arguments](#arguments)).
then the translator should try to use the other registers for the variables.    
if there are not enugh registers for all variables:
+ if the [Arguments](#arguments) where passed using a pointer stored in `X20` the memory block of the arguments is resized
+ otherwise a new memory block will be allocated and stored in `X00`.
    + the newly allocated memory block will be large enugh to store all local variables, which are not stored in registers
    + the memory block will be freed before the method returns

### Operant Stack
the _PVM_ has no operant stack. thus the operant stack is removed at translation time.    
the translator has to find out for what the operant stack is used for and then translate the given operations without the operant stack.

the order and effective usage of the following must remain:
+ method invocations
+ field (and array element) accesses/assignments/modifications

for the outside it must look like the original bytecode whould be interpreted
