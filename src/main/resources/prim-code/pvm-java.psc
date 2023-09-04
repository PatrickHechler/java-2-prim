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
|		unum entries_size; // note that size contains the size (in bytes) of the entries array
|		num[] entries;
|	}
|:>
|:	struct hashset {
|		num# entries; // if negative the set is empty     // off = HEX-0
|		unum maxi;                                        // off = HEX-8
|		unum entry_count;                                 // off = HEX-10
|		func (num a, num b) --> <num equal> equalizer;    // off = HEX-18
|			X03: c --> c
|			X04: a --> equal (0 = not-equal)
|			X05: b --> b
|			X06..X0F: n --> n // both inclusive
|		func (num val) --> <unum hash> hashmaker;         // off = HEX-20
|			X03: c --> c
|			X04: val --> hash
|			X05..X0F: n --> n // both inclusive
|	}
|:>
#EXP~hashset_SIZE HEX-28
#EXP~hashset_maxi_OFF HEX-8
#EXP~hashset_entry_count_OFF HEX-10
#EXP~hashset_equalizer_OFF HEX-18
#EXP~hashset_hashmaker_OFF HEX-20
|:	func hashset_get(struct hashset# set, unum hash, num val) --> <struct hashset# set_, num result>
|	X03: set --> set
|	X04: hash --> result (negative: not found; other: value)
|	X05: val --> ?
|	X06: --> ?
|	X07: --> ?
|	other registers may also be modified if set#.equalizer modifies them
|:>
#EXP~hashset_get_POS --POS--
	MOV X07, [X03]
	SGN X07
	JMPLE returnX04_m1
	AND X04, [X03 + hashset_maxi_OFF]
	LSH X04, 3
	MOV X04, [X07 + X04]
	CMP X04, -1
	JMPGT hs_get_check_result
	JMPEQ return
	NOT X04
	MOV X06, X04
	MOV X07, [X06]
	@hs_get_list_loop
		MOV X04, [X06 + X07]
		CALNO [X03 + hashset_equalizer_OFF]
		JMPERR return
		SGN X04
		JMPNE hs_get_list_loop_found
		SUB X07, 8
		JMPZC hs_get_list_loop
		MOV X04, -1
		RET
		@hs_get_list_loop_found
			MOV X04, [X06 + X07]
			RET
	@hs_get_check_result
		CALNO [X03 + hashset_equalizer_OFF]
		JMPERR return
		SGN X04
		JMPEQ returnX04_m1
		MOV X04, X06
		RET
	@returnX04_m1
		MOV X04, -1
		RET

|:	func hashset_add(struct hashset# set, unum hash, num addval) --> <struct hashset# set_, num value>
|	X00 .. X02 --> ?
|	X03: set --> set_
|	X04: hash --> oldval
|	X05: newval --> ?
|	X06 .. X --> ?
|	other registers may also be modified if set#.equalizer or set#.hashmaker modifies them
|:>
#EXP~hashset_add_POS --POS--
	MOV X07, 1
	JMP hashset_add_put_impl
|:	func hashset_put(struct hashset# set, unum hash, num newval) --> <struct hashset# set_, num oldval>
|	X00 .. X02 --> ?
|	X03: set --> set_
|	X04: hash --> oldval
|	X05: putval --> ?
|	X06 .. X --> ?
|	other registers may also be modified if set#.equalizer or set#.hashmaker modifies them
|:>
#EXP~hashset_put_POS --POS--
	XOR X07, X07
@hashset_add_put_impl
	|:	X03: set
	|	X04: hash
	|	X05: newval
	|	X06: set#.entries
	|	X07: is_add
	|:>
	MOV X06, [X03]
	SGN X06
	JMPLE hs_add_put__create_set
	MOV X08, [X03 + hashset_maxi_OFF]
	RLSH X08, 1
	MOV X09, [X03 + hashset_entry_count_OFF]
	CMP X08, X09
	JMPGT hs_add_put__no_grow
	LSH X08, 2
	OR X08, 3
	|:	X03: set
	|	X04: hash
	|	X05: newval
	|	X06: set#.entries
	|	X07: is_add
	|	X08: new set#.maxi ((set#.maxi << 1) | 1)
	|	X09: set#.entry_count
	|:>
	MVAD X00, X08, 1
	LSH X00, 3
	MOV X01, HEX-FF
	MOV X02, X00
	INT INT_MEMORY_ALLOC
	JMPERR return
	INT INT_MEM_BSET
	MOV X0A, X00
	MVAD X09, X08, -8
	RLSH X09, 1
	MOV X0B, X04
	|:	X03: set
	|	X04: -
	|	X05: newval
	|	X06: set#.entries
	|	X07: is_add
	|	X08: new set#.maxi ((set#.maxi << 1) | 1)
	|	X09: loop offset
	|	X0A: new set#.entries
	|	X0B: hash
	|:>
	@hs_add_put__grow_loop
		MOV X0C, [X06 + X09]
		CMP X0C, -1
		JMPEQ hs_add_put__grow_loop__fin_iter
		JMPLT hs_add_put__grow_loop__list
		MOV X04, X0C
		CALNO [X03 + hashset_hashmaker_OFF]
		JMPERR return
		AND X04, X08
		LSH X04, 3
		MOV X0D, [X0A + X04]
		CMP X0D, -1
		JMPGT hs_add_put__grow_loop__entry__create_list
		JMPLT hs_add_put__grow_loop__entry__add_list
		|:	X03: set
		|	X04: new offset of current loop entry
		|	X05: newval
		|	X06: set#.entries
		|	X07: is_add
		|	X08: new set#.maxi ((set#.maxi << 1) | 1)
		|	X09: loop offset
		|	X0A: new set#.entries
		|	X0B: hash
		|	X0C: current loop entry ([X06 + X09])
		|	X0D: (new set#.entries)[current loop entries index] ([X0A + X04])
		|:>
		MOV [X0A + X04], X0C
		JMP hs_add_put__grow_loop__fin_iter
		|:	X08: (not) new set#.maxi ((set#.maxi << 1) | 1)
		|	X0A: (not) new set#.entries
		|	frees all lists in the not new set#.entries and the not new set#.entries. then returns to caller
		|:>
		@hs_add_put__grow_loop__ERROR
			LSH X08, 3
			@hs_add_put__grow_loop__ERROR_loop
				MOV X00, [X0A + X08]
				CMP X00, -1
				JMPGE hs_add_put__grow_loop__ERROR_loop__iter_end
				NOT X00
				INT INT_MEMORY_FREE
				@hs_add_put__grow_loop__ERROR_loop__iter_end
					USUB X08, 8
					JMPCC hs_add_put__grow_loop__ERROR_loop
			MOV X00, X0A
			INT INT_MEMORY_FREE
			RET
		|:	X03: set
		|	X04: new offset of current loop entry
		|	X05: newval
		|	X06: set#.entries
		|	X07: is_add
		|	X08: new set#.maxi ((set#.maxi << 1) | 1)
		|	X09: loop offset
		|	X0A: new set#.entries
		|	X0B: hash
		|	X0C: current loop entry ([X06 + X09])
		|	X0D: (new set#.entries)[current loop entries index] ([X0A + X04])
		|:>
		@hs_add_put__grow_loop__entry__create_list
			MOV X00, 32
			INT INT_MEMORY_ALLOC
			JMPERR return
			MOV [X00], 8
			MOV [X00 + 8], X0D
			MOV [X00 + 16], X0C
			NOT X00
			MOV [X0A + X04], X00
			JMP hs_add_put__grow_loop__fin_iter
		|:	X03: set
		|	X04: new offset of current loop entry
		|	X05: newval
		|	X06: set#.entries
		|	X07: is_add
		|	X08: new set#.maxi ((set#.maxi << 1) | 1)
		|	X09: loop offset
		|	X0A: new set#.entries
		|	X0B: hash
		|	X0C: current loop entry ([X06 + X09])
		|	X0D: (new set#.entries)[current loop entries index] ([X0A + X04]) (logical not of a hs_list)
		|:>
		@hs_add_put__grow_loop__entry__add_list
			NOT X0D
			ADD [X0D], 8
			MOV X0E, [X0D]
			MVAD X03, X0E, -1
			BCP X0D, X03
			JMPSB hs_add_put__grow_loop__entry__add_list__no_list_grow
			MOV X00, X0D
			MOV X01, X0E
			LSH X01, 1
			INT INT_MEMORY_REALLOC |> on error undo the decrease list size
			JMPERR hs_add_put__grow_loop__ERROR
			MOV X0D, X00
			NOT X00
			MOV [X0A + X04], X00
			@hs_add_put__grow_loop__entry__add_list__no_list_grow
				MOV [X0D + X0E], X0C
				JMP hs_add_put__grow_loop__fin_iter
		|:	X03: set
		|	X04: -
		|	X05: newval
		|	X06: set#.entries
		|	X07: is_add
		|	X08: new set#.maxi ((set#.maxi << 1) | 1)
		|	X09: loop offset
		|	X0A: new set#.entries
		|	X0B: hash
		|	X0C: current loop entry ([X06 + X09])
		|:>
		@hs_add_put__grow_loop__list
			NOT X0C
			MOV X0D, [X0C]
			|:	X03: set
			|	X04: -
			|	X05: newval
			|	X06: set#.entries
			|	X07: is_add
			|	X08: new set#.maxi ((set#.maxi << 1) | 1)
			|	X09: loop offset
			|	X0A: new set#.entries
			|	X0B: hash
			|	X0C: current loop entry ([X06 + X09])
			|	X0D: current loop entry list offset (inner loop offset)
			|:>
			@hs_add_put__grow_loop__list__loop
				MOV X0E, [X0C + X0D]
				MOV X04, X0E
				CALNO [X03 + hashset_hashmaker_OFF]
				JMPERR return
				AND X04, X08
				LSH X04, 3
				MOV X0F, [X0A + X04]
				CMP X0F, -1
				|:	X03: set
				|	X04: offset in new set#.entries
				|	X05: newval
				|	X06: set#.entries
				|	X07: is_add
				|	X08: new set#.maxi ((set#.maxi << 1) | 1)
				|	X09: loop offset
				|	X0A: new set#.entries
				|	X0B: hash
				|	X0C: current loop entry ([X06 + X09])
				|	X0D: current loop entry list offset (inner loop offset)
				|	X0E: current inner loop entry ([X0C + X0D]) (add this to the new entries)
				|	X0F: (new set#.entries)[index in new set#.entries] ([X0A + X04])
				|:>
				JMPGT hs_add_put__grow_loop__list__loop__create_list
				JMPLT hs_add_put__grow_loop__list__loop__add_list
				MOV [X0A + X04], X0E
				JMP hs_add_put__grow_loop__list__loop__iter_end
				@hs_add_put__grow_loop__list__loop__add_list
					NOT X0F
					ADD [X0F], 8
					MOV X03, [X0F]
					MVAD X02, X03, -1
					BCP X03, X02
					JMPSB hs_add_put__grow_loop__list__loop__add_list__no_grow
					MOV X01, X03
					LSH X01, 1
					MOV X00, X0F
					INT INT_MEMORY_REALLOC
					JMPERR hs_add_put__grow_loop__ERROR
					MOV X0F, X00
					NOT X00
					MOV [X0A + X04], X00
					@hs_add_put__grow_loop__list__loop__add_list__no_grow
						MOV [X0F + X03], X0E
						JMP hs_add_put__grow_loop__list__loop__iter_end
				@hs_add_put__grow_loop__list__loop__create_list
					MOV X00, 32
					INT INT_MEMORY_ALLOC
					JMPERR return
					MOV [X00], 8
					MOV [X00 + 8], X0F
					MOV [X00 + 16], X0E
					NOT X00
					MOV [X0A + X04], X00
				@hs_add_put__grow_loop__list__loop__iter_end
					SUB X0D, 8
					JMPZC hs_add_put__grow_loop__list__loop
		@hs_add_put__grow_loop__fin_iter
			USUB X09, 8
			JMPCC hs_add_put__grow_loop
	|:	X03: set
	|	X04: -
	|	X05: newval
	|	X06: set#.entries
	|	X07: is_add
	|	X08: new set#.maxi ((set#.maxi << 1) | 1)
	|	X09: loop offset
	|	X0A: new set#.entries
	|	X0B: hash
	|:>
	MOV X04, X0B
	MOV X0B, X08
	SWAP [X03 + hashset_maxi_OFF], X0B
	MOV [X03], X0A
	SWAP X0A, X06
	MOV X09, [X03 + hashset_entry_count_OFF]
	|:	X03: set
	|	X04: hash
	|	X05: newval
	|	X06: set#.entries
	|	X07: is_add
	|	X08: set#.maxi
	|	X09: set#.entry_count
	|	X0A: old set#.entries
	|	X0B: old set#.maxi
	|:>
	LSH X0B, 3
	@hs_add_put__free_old_loop
		MOV X00, [X0A + X0B]
		CMP X00, -1
		JMPGE hs_add_put__free_old_loop__fin_iter
		NOT X00
		INT INT_MEMORY_FREE
		@hs_add_put__free_old_loop__fin_iter
			USUB X0B, 8
			JMPCC hs_add_put__free_old_loop
	MOV X00, X0A
	INT INT_MEMORY_FREE
	AND X04, X08
	JMP hs_add_put__after_grow
	|:	X03: set
	|	X04: hash
	|	X05: newval
	|	X06: set#.entries
	|	X07: is_add
	|	X08: set#.maxi >>> 1
	|	X09: set#.entry_count
	|:>
	@hs_add_put__no_grow
		AND X04, [X03 + hashset_maxi_OFF]
	@hs_add_put__after_grow
		LSH X04, 3
		MOV X0A, [X06 + X04]
		CMP X0A, -1
		JMPGT hs_add_put__check_entry
		JMPLT hs_add_put__check_list
		MOV [X06 + X04], X05
		MOV X04, -1
		RET
		@hs_add_put__check_entry
			MOV X0B, X04
			MOV X04, X0A
			CALNO [X03 + hashset_equalizer_OFF]
			JMPERR return
			SGN X04
			JMPNE hs_add_put__check_entry__found
			MOV X00, 32
			INT INT_MEMORY_ALLOC
			JMPERR return
			MOV [X00], 8
			MOV [X00 + 8], X0A
			MOV [X00 + 16], X05
			INC [X03 + hashset_entry_count_OFF]
			MOV X04, -1
			RET
			@hs_add_put__check_entry__found
				SGN X07
				JMPEQ hs_add_put__check_entry__found__only_add
				MOV [X06 + X0B], X05
				@hs_add_put__check_entry__found__only_add
					MOV X04, X0A
					RET
		|:	X03: set
		|	X04: offset of the element ((newval.hash & set#.maxi) << 3)
		|	X05: newval
		|	X06: set#.entries
		|	X07: is_add
		|	X08: -
		|	X09: set#.entry_count
		|	X0A: set#.entries[newval.hash & set#.maxi] ([X06 + X04])
		|:>
		@hs_add_put__check_list
			NOT X0A
			MOV X08, [X0A]
			MOV X0B, X04
			@hs_add_put__check_list__loop
				MOV X04, [X0A + X08]
				CALNO [X03 + hashset_equalizer_OFF]
				JMPERR return
				SGN X04
				JMPNE hs_add_put__check_list__loop__found
				@hs_add_put__check_list__loop__iter_end
					SUB X08, 8
					JMPZC hs_add_put__check_list__loop
			ADD [X0A], 8
			MOV X01, [X0A]
			MVAD X02, X01, -1
			BCP X01, X02
			JMPSB hs_add_put__check_list__no_realloc
				MOV X00, X0A
				INT INT_MEMORY_REALLOC
				JMPERR return
				MOV X0A, X00
				NOT X00
				MOV [X06 + X0B], X00
			@hs_add_put__check_list__no_realloc
			MOV [X0A + X01], X05
			MOV X04, -1
			INC [X03 + hashset_entry_count_OFF]
			RET
			|:	X03: set
			|	X04: -
			|	X05: newval
			|	X06: set#.entries
			|	X07: is_add
			|	X08: current offset in list
			|	X09: set#.entry_count
			|	X0A: set#.entries[newval.hash & set#.maxi] ([X06 + X04])
			|	X0B: offset of the element ((newval.hash & set#.maxi) << 3)
			|:>
			@hs_add_put__check_list__loop__found
				SGN X07
				JMPEQ return
				SGN X07
				JMPNE hs_add_put__check_list__loop__found__only_add
				MOV X04, X05
				SWAP [X0A + X08], X04
				RET
				@hs_add_put__check_list__loop__found__only_add
					MOV X04, [X0A + X08]
					RET
	|:	X03: set
	|	X04: hash
	|	X05: newval
	|	X06: set#.entries
	|	X07: is_add
	|:>
	@hs_add_put__create_set
		MOV X00, HEX-40 |> 64 bytes : 8 entries
		MOV X01, HEX-FF
		MOV X02, X00
		INT INT_MEMORY_ALLOC
		JMPERR return
		INT INT_MEM_BSET
		AND X04, 7 |> calc offset ((hash & (8-1)) << 3)
		LSH X04, 3
		MOV [X00 + X04], X05 |> set value
		MOV [X03], X00
		MOV [X03 + hashset_maxi_OFF], 7
		MOV [X03 + hashset_entry_count_OFF], 1
		@return_X04_m1
			MOV X04, -1 |> there was no old value
			RET

|:	func hashset_remove(struct hashset# set, unum hash, num oldval) --> <struct hashset# set_, num oldval>
|	X03: set --> set_
|	X04: hash --> oldval
|	X05: remval --> ?
|:>
#EXP~hashset_remove_POS --POS--
	|:	X03: set
	|	X04: hash
	|	X05: remval
	|	X06: set#.entries
	|:>
	MOV X06, [X03]
	SGN X06
	JMPLE return_X04_m1
	AND X04, [X03 + hashset_maxi_OFF]
	LSH X04, 3
	MOV X07, [X03 + X04]
	CMP X07, -1
	JMPGT hs_rem__check_entry
	JMPEQ return_X04_m1
	NOT X07
	MOV X08, [X07]
	MOV X09, X04
	|:	X03: set
	|	X04: -
	|	X05: remval
	|	X06: set#.entries
	|	X07: list
	|	X08: list offset
	|	X09: offset
	|:>
	@hs_rem__check_list__loop
		MOV X04, [X07 + X08]
		CALNO [X03 + hashset_equalizer_OFF]
		JMPERR return
		SGN X04
		JMPNE hs_rem__check_list__loop__found
		SUB X08, 8
		JMPZC hs_rem__check_list__loop
	MOV X04, -1
	RET
		|:	X03: set
		|	X04: -
		|	X05: remval
		|	X06: set#.entries
		|	X07: list
		|	X08: list offset
		|	X09: offset
		|:>
		@hs_rem__check_list__loop__found
			MOV X04, [X07 + X08]
			MOV X02, [X07]
			CMP X02, 16
			JMPEQ hs_rem__check_list__loop__found__remove_list
			JMPLT hs_rem__check_list__loop__found__remove_entire_list
				SUB X02, X08
				SUB X02, 8
				JMPZS hs_rem__check_list__loop__found__shrink_list__no_mem_move
					MOV X01, X07 |> skip if the last entry is removed
					ADD X01, X08
					MVAD X00, X07, 8
					ADD X00, X08
					INT INT_MEM_MOVE
				@hs_rem__check_list__loop__found__shrink_list__no_mem_move
				SUB [X07], 8
				MOV X01, [X07]
				MVAD X02, X01, -1
				BCP X02, X01
				JMPSB hs_rem_shrink
				ADD X01, 8
				MOV X00, X07
				INT INT_MEMORY_REALLOC
				NOT X00
				MOV [X06 + X09], X00
				JMP hs_rem_shrink
			@hs_rem__check_list__loop__found__remove_list
				CMP X08, 8
				JMPEQ hs_rem__check_list__loop__found__remove_list__off_is_8
				MOV X08, 8
				JMP hs_rem__check_list__loop__found__remove_list__new_off
				@hs_rem__check_list__loop__found__remove_list__off_is_8
					MOV X08, 16
				@hs_rem__check_list__loop__found__remove_list__new_off
				MOV [X06 + X09], [X07 + X08]
				MOV X00, X07
				INT INT_MEMORY_FREE
				JMP hs_rem_shrink
			@hs_rem__check_list__loop__found__remove_entire_list
				|> this should never happen
				MOV [X06 + X09], -1
				MOV X00, X07
				INT INT_MEMORY_FREE
				JMP hs_rem_shrink
	|:	X03: set
	|	X04: offset
	|	X05: remval
	|	X06: set#.entries
	|	X07: entry
	|:>
	@hs_rem__check_entry
		SWAP X04, X07
		CALNO [X03 + hashset_equalizer_OFF]
		JMPERR return
		SGN X04
		JMPEQ return_X04_m1
		MOV X04, -1
		SWAP [X03 + X07], X04
	|:	X03: set
	|	X04: oldvalue
	|	X05: -
	|	X06: set#.entries
	|:>
	@hs_rem_shrink
		DEC [X03 + hashset_entry_count_OFF]


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
