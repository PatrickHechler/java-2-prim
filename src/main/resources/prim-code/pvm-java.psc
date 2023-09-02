~IF #~ME
	#pvm_java_INIT 0
	#pvm_java_MAIN 0
	#pvm_java_INIT_ADD_POS 0
	#JNI_Env_ADD_POS 0
~ELSE
	~READ_SYM "constants.psf" >
	~READ_SYM "internal-structures.psf" >
	~READ_SYM "[THIS]" --MY_CONSTS-- #ADD~ME 1 >
~ENDIF
:
	pvm_java_INIT
	pvm_java_MAIN
>

|>	hash set
|:	struct hs_list {
|		unum size; // note that size contains the size of the structure
|		num[] entries;
|	}
|:>
|:	struct hashset {
|		num# entries;                                     // off = HEX-0
|		unum maxi; // if set to zero the set size is zero // off = HEX-8
|		unum entry_count;                                 // off = HEX-10
|		func (num a, num b) --> <num equal> equalizer;    // off = HEX-18
|			X00: c --> c
|			X01: a --> equal (0 = not-equal)
|			X02: b --> b
|		func (num val) --> <unum hash> hashmaker;         // off = HEX-20
|			X00: c --> c
|			X01: val --> hash
|	}
|:>
#EXP~hashset_SIZE HEX-28
#EXP~hashset_hashmaker_OFF HEX-20
#EXP~hashset_equalizer_OFF HEX-18
#EXP~hashset_entry_count_OFF HEX-10
#EXP~hashset_maxi_OFF HEX-8
|:	func hashset_get(struct hashset# set, unum hash, num val) --> <struct hashset# set_, num result>
|	X00: set --> set
|	X01: hash --> result (negative: not found; other: value)
|	X02: val
|	X03, X04: --> ?
|:>
#EXP~hashset_get_POS --POS--
	AND X01, [X00, hashset_maxi_OFF]
	LSH X01, 3
	MOV X01, [X00 + X01]
	CMP X01, -1
	JMPGT hs_get_check_result
	JMPEQ return
	NOT X01
	MOV X03, X01
	MVAD X04, [X03], -8
	@hs_get_list_loop
		MOV X01, [X03 + X04]
		CALNO [X00 + hashset_equalizer_OFF]
		SGN X01
		JMPNE hs_get_list_loop_found
		SUB X04, 8
		JMPZC hs_get_list_loop
	MOV X01, -1
	RET
		@hs_get_list_loop_found
			MOV X01, [X03 + X04]
			RET
	@hs_get_check_result
		MOV X03, X01
		CALNO [X00 + hashset_equalizer_OFF]
		SGN X01
		JMPEQ returnX01_m1
		MOV X01, X03
		RET
	@returnX01_m1
		MOV X01, -1
		RET
|:	func hashset_put(struct hashset# set, unum hash, num newval) --> <struct hashset# set_, num oldval>
|	X00: set --> set_
|	X01: hash --> oldval
|	X02: newval
|:>
#EXP~hashset_put_POS --POS--
	|> TODO implement
|:	func hashset_add(struct hashset# set, unum hash, num newval) --> <struct hashset# set_, num value>
|	X00: set --> set_
|	X01: hash --> value
|	X02: newval
|:>
#EXP~hashset_add_POS --POS--
	|> TODO implement
|:	func hashset_remove(struct hashset# set, unum hash, num oldval) --> <struct hashset# set_, num oldval>
|	X00: set --> set_
|	X01: hash --> oldval
|	X02: newval
|:>
#EXP~hashset_remove_POS --POS--
	|> TODO implement




#native_throw_POS --POS--
: -1 >
#native_exec_class_POS --POS--
: -1 >
#local_reference_size_POS --POS--
: 0 >
#local_references_adr_POS --POS--
: -1 >

|:	creates a new local reference and let it refer to the X00 value
|	if needed the current frame will grow
|	parameter: X00: points to the instance
|	results: X00: the local-reference, ERRNO: zero on success otherwise non-zero
|	modifies: X01, X02
|:>
@add_local_reference
	#REL_POS ( local_reference_size_POS - --POS-- )
	MOV X01, [IP + REL_POS]
	SGN X01
	JMPEQ create_local_frame
	#REL_POS ( local_references_adr_POS - --POS-- )
	MOV X02, [IP + REL_POS]
	@add_local_reference_loop
		SUB X01, 8
		SGN [X02 + X01]
		JMPLT found_free_reference
		SGN X01
		JMPGT add_local_reference_loop
	|> no unused ref found, grow by 4 references
	ADD X01, 32
	SWAP X00, X02
	INT INT_MEMORY_REALLOC
	JMPERR return
	#REL_POS ( local_references_adr_POS - --POS-- )
	MOV [IP + REL_POS], X00
	#REL_POS ( local_reference_size_POS - --POS-- )
	MOV [IP + REL_POS], X01
	ADD X00, X01
	SUB X00, 32
	MOV [X00], X02
	RET
	@found_free_reference
		MOV [X02 + X01], X00
		MOV X02, X00
		ADD X00, X01
		RET
	@create_local_frame
		MOV X01, X00
		MOV X00, 128
		INT INT_MEMORY_ALLOC
		JMPERR return
		#REL_POS ( local_references_adr_POS - --POS-- )
		MOV [IP + REL_POS], X00
		#REL_POS ( local_reference_size_POS - --POS-- )
		MOV [IP + REL_POS], 128
		MOV [X00], X01
		RET

|:	X00 stores a reference to the exception instance which should been thrown
|	ERRNO will be set to ERR_JAVA_THROW
|	if there is already an exception instance which is being 
|:>
#JNI_Env_throw ( --POS-- - JNI_Env_ADD_POS )
	MOV ERRNO, ERR_JAVA_THROW
	#REL_POS ( native_throw_POS - --POS-- )
	MOV [IP + REL_POS], [X00]
	@return
	RET
#JNI_Env_isThrowing ( --POS-- - JNI_Env_ADD_POS )
	#REL_POS ( native_throw_POS - --POS-- )
	SGN [IP + REL_POS]
	JMPLT returnX00_0
	MOV X00, 1
	RET
	@returnX00_0
		XOR X00, X00
		RET
#JNI_Env_getThrowingClass ( --POS-- - JNI_Env_ADD_POS )
	#REL_POS ( native_throw_POS - --POS-- )
	MOV X00, [IP + REL_POS]
	SGN X00
	JMPLT returnX00_m1
	MOV X00, [X00]
	JMP add_local_reference
	@returnX00_m1
		MOV X00, -1
		RET
#JNI_Env_catch ( --POS-- - JNI_Env_ADD_POS )
	MOV X00, -1
	#REL_POS ( native_throw_POS - --POS-- )
	SWAP X00, [IP + REL_POS]
	SGN X00
	JMPLT return
	JMP add_local_reference
#JNI_Env_findModule ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_findClass ( --POS-- - JNI_Env_ADD_POS )
	SGN X01
	JMPGT JNI_Env_findClass_module_found
	#REL_POS ( native_exec_class_POS - --POS-- )
	MOV X01, [IP + REL_POS]
	MOV X01, [X01 + object_class_instance_module_OFF]
	@JNI_Env_findClass_module_found
		
		RET
#JNI_Env_findInstanceMethod ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_findStaticMethod ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_findInstanceField ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_findStaticField ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_invokeInstance ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_invokeStatic ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_invokeSuper ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_new ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_getStaticField ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_getInstanceField ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_putStaticField ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_putInstanceField ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_createLocalReference ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_createGlobalReference ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_createWeakReference ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_removeReference ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_ensureFrameSize ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_growFrame ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET
#JNI_Env_shrinkFrame ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_POS --POS--
:
|> GENERATED-CODE-START
|> this code-block is automatic generated, do not modify
	JNI_Env_throw
	JNI_Env_isThrowing
	JNI_Env_getThrowingClass
	JNI_Env_catch
	JNI_Env_findModule
	JNI_Env_findClass
	JNI_Env_findInstanceMethod
	JNI_Env_findStaticMethod
	JNI_Env_findInstanceField
	JNI_Env_findStaticField
	JNI_Env_invokeInstance
	JNI_Env_invokeStatic
	JNI_Env_invokeSuper
	JNI_Env_new
	JNI_Env_getStaticField
	JNI_Env_getInstanceField
	JNI_Env_putStaticField
	JNI_Env_putInstanceField
	JNI_Env_createLocalReference
	JNI_Env_createGlobalReference
	JNI_Env_createWeakReference
	JNI_Env_removeReference
	JNI_Env_ensureFrameSize
	JNI_Env_growFrame
	JNI_Env_shrinkFrame

|> here is the end of the automatic generated code-block
|> GENERATED-CODE-END
>
#JNI_Env_LEN ( --POS-- - JNI_Env_POS )

#EXP~pvm_java_INIT --POS--
	#REL_POS ( JNI_Env_POS - --POS-- )
	LEA X1F, REL_POS
	PUSH X00
	XOR X00, X00
	@pvm_java_INIT_JNI_env_loop
	~IF #~ME
		#EXP~JNI_Env_ADD_POS --POS--
	~ENDIF
	ADD [X1F + X00], IP
	ADD X00, 8
	CMP X00, JNI_Env_LEN
	JMPLT pvm_java_INIT_JNI_env_loop
	POP X00
	RET

#EXP~pvm_java_MAIN --POS--
	|> TODO implement
