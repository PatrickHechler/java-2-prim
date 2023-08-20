# Java-2-Prim
## Method call
### JNI Argument
#### Java code calls
the _JNI-Env_ pointer is passed in the `X1F` register    
+ if a non-`native` method is invoked, the _JNI-Env_ pointer also has to be stored in the `X1F` register when the method returns
+ otherwise _pvm-java_ sets the `X1F` register to the _JNI-Env_ pointer after the native code returns
	+ the native code __must__ not modify the _JNI-Env_ pointers target/content
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
### Return Value
if there is a return value it is stored in the `X00` register
## Local Variables
they are stored like the [arguments](#arguments)
also see the [arguments](#java-2-prim)
[test](https://hechler.de)