^+v::
	SetKeyDelay, 1
	Loop, read, output.txt
	{
		Loop, parse, A_LoopReadLine, %A_Tab%
		{
			SendInput, %A_LoopField%
			Send, {tab}
		}
		Send, {down}{left}{left}
	}

	ExitApp 
return